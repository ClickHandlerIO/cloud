package io.clickhandler.sql;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractIdleService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.Vertx;
import javaslang.control.Try;
import org.h2.server.TcpServer;
import org.h2.tools.Server;
import org.jooq.*;
import org.jooq.Table;
import org.jooq.conf.*;
import org.jooq.impl.*;
import org.jooq.tools.StringUtils;
import org.jooq.tools.jdbc.JDBCUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Scheduler;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Clay Molocznik
 */
public class SqlDatabase extends AbstractIdleService implements SqlExecutor {
    private static final int SANE_MAX = 2000;
    private static final int DEV_POOL_SIZE = 15;
    private static final int TEST_POOL_SIZE = 15;
    private static final int PROD_POOL_SIZE = 100;
    private static final int PROD_READ_POOL_SIZE = 100;
    private static final int PROD_MYSQL_PREPARE_STMT_CACHE_SIZE = 4096;
    private static final int DEV_MYSQL_PREPARE_STMT_CACHE_SIZE = 256;
    private static final int PROD_MYSQL_PREPARE_STMT_CACHE_SQL_LIMIT = 4096;
    private static final int DEV_MYSQL_PREPARE_STMT_CACHE_SQL_LIMIT = 2048;
    private static final String ENTITY_PACKAGE = "io.clickhandler.sql";
    private static final Logger LOG = LoggerFactory.getLogger(SqlDatabase.class);

    protected final SqlConfig config;
    protected final String name;
    protected final HikariConfig hikariConfig;
    protected final HikariConfig hikariReadConfig;
    protected final Set<Class<?>> entityClasses = new HashSet<>();
    protected final Vertx vertx;
    private final ThreadLocal<SqlSession> sessionLocal = new ThreadLocal<>();
    private final Map<Class, Mapping> mappings = new HashMap<>();
    private final Map<String, TableMapping> tableMappingsByName = Maps.newHashMap();
    private final Map<Class, TableMapping> tableMappings = Maps.newHashMap();
    private final Map<Class, TableMapping> tableMappingsByEntity = Maps.newHashMap();
    private final List<TableMapping> tableMappingList = Lists.newArrayList();
    private final String[] entityPackageNames;
    private final String[] jooqPackageNames;
    private final Map<String, Table> jooqMap = Maps.newHashMap();
    private final Reflections[] entityReflections;
    private final Reflections[] jooqReflections;
    protected Configuration configuration;
    protected Configuration readConfiguration;
    protected HikariDataSource dataSource;
    protected HikariDataSource readDataSource;
    protected SqlSchema sqlSchema;
    protected SqlPlatform dbPlatform;
    protected Settings settings;
    protected H2Server h2Server;
    protected ExecutorService writeExecutor;
    protected ExecutorService readExecutor;
    protected Scheduler observableScheduler;

    public SqlDatabase(
        Vertx vertx,
        SqlConfig config,
        String[] entityPackageNames,
        String[] jooqPackageNames) {
        entityPackageNames = entityPackageNames == null ? new String[0] : entityPackageNames;
        jooqPackageNames = jooqPackageNames == null ? new String[0] : jooqPackageNames;
        this.vertx = vertx;
        this.config = Preconditions.checkNotNull(config, "config must be set.");
        this.name = Strings.nullToEmpty(config.getName()).trim();
        this.entityPackageNames = entityPackageNames;
        this.jooqPackageNames = jooqPackageNames;

        observableScheduler = io.vertx.rxjava.core.RxHelper.scheduler(vertx);

        final List<Reflections> entityReflections = new ArrayList<>();
        final List<Reflections> jooqReflections = new ArrayList<>();

        if (config.isGenerateSchema()) {
            // Add all look paths for entities.
            for (String entityPackageName : entityPackageNames) {
                entityReflections.add(new Reflections(entityPackageName));
            }

            entityReflections.add(new Reflections(ENTITY_PACKAGE));
        } else {
            // Add all look paths for entities.
            for (String entityPackageName : entityPackageNames) {
                entityReflections.add(new Reflections(entityPackageName));
            }
            // Add all look paths for jOOQ schema.
            for (String jooqPackageName : jooqPackageNames) {
                jooqReflections.add(new Reflections(jooqPackageName));
            }

            // Add Core Entity path.
            entityReflections.add(new Reflections(ENTITY_PACKAGE));
            // Add AMP jOOQ path.
//            jooqReflections.add(new Reflections(SCHEMA_PACKAGE));
        }

        this.entityReflections = entityReflections.toArray(new Reflections[entityReflections.size()]);
        this.jooqReflections = jooqReflections.toArray(new Reflections[jooqReflections.size()]);

        final String jdbcUrl = Strings.nullToEmpty((config.getUrl())).trim();
        final String jdbcUser = Strings.nullToEmpty((config.getUser()));
        final String jdbcPassword = Strings.nullToEmpty((config.getPassword()));

        final String jdbcReadUrl = Strings.nullToEmpty((config.getReadUrl()));
        final String jdbcReadUser = Strings.nullToEmpty((config.getReadUser()));
        final String jdbcReadPassword = Strings.nullToEmpty((config.getReadPassword()));
        final SQLDialect dialect = JDBCUtils.dialect(jdbcUrl);

        // Configure connection pool.
        hikariConfig = new HikariConfig();
        hikariReadConfig = new HikariConfig();

        // Always set auto commit to off.
        hikariConfig.setAutoCommit(false);
        hikariConfig.setRegisterMbeans(true);

        hikariReadConfig.setReadOnly(true);
        hikariReadConfig.setAutoCommit(false);
        hikariReadConfig.setRegisterMbeans(true);

        // Set the default maximum pool size.
        if (config.isDev()) {
            hikariConfig.setMaximumPoolSize(DEV_POOL_SIZE);
            hikariReadConfig.setMaximumPoolSize(DEV_POOL_SIZE);
        } else if (config.isTest()) {
            hikariConfig.setMaximumPoolSize(TEST_POOL_SIZE);
            hikariReadConfig.setMaximumPoolSize(TEST_POOL_SIZE);
        } else {
            hikariConfig.setMaximumPoolSize(PROD_POOL_SIZE);
            hikariReadConfig.setMaximumPoolSize(PROD_READ_POOL_SIZE);
        }

        // Sanitize Max Pool size.
        if (config.getMaxPoolSize() > 0 && config.getMaxPoolSize() <= SANE_MAX) {
            hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
        } else {
            LOG.warn("An Invalid MaxPoolSize value was found. Found '" + config.getMaxPoolSize() + "' but set to a more reasonable '" + SANE_MAX + "'");
            hikariConfig.setMaximumPoolSize(SANE_MAX);
        }
        if (config.getMaxReadPoolSize() > 0 && config.getMaxReadPoolSize() < SANE_MAX) {
            hikariReadConfig.setMaximumPoolSize(config.getMaxReadPoolSize());
        } else {
            LOG.warn("An Invalid MaxReadPoolSize value was found. Found '" + config.getMaxPoolSize() + "' but set to a more reasonable '" + SANE_MAX + "'");
            hikariReadConfig.setMaximumPoolSize(SANE_MAX);
        }

        // Always use READ_COMMITTED.
        // Persist depends on this isolation level.
        hikariConfig.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        hikariReadConfig.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        // Set Connection Test Query.
        // This is valid SQL and is supported by any "ACTUAL" SQL database engine.
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariReadConfig.setConnectionTestQuery("SELECT 1");

        // Configure jOOQ settings.
        settings = new Settings();
        settings.setRenderSchema(false);
        settings.setExecuteWithOptimisticLocking(true);
        settings.setRenderNameStyle(RenderNameStyle.QUOTED);
        settings.setRenderKeywordStyle(RenderKeywordStyle.UPPER);
        settings.setReflectionCaching(true);
        settings.setParamType(ParamType.INDEXED);
        settings.setAttachRecords(true);
        settings.setBackslashEscaping(BackslashEscaping.DEFAULT);
        settings.setStatementType(StatementType.PREPARED_STATEMENT);

        // Init jOOQ Configuration.
        configuration = new DefaultConfiguration();
        configuration.set(new RecordMapperProviderImpl());
        configuration.set(settings);
        configuration.set(dialect);

        configuration.set(PrettyPrinter::new, DeleteOrUpdateWithoutWhereListener::new);

        ////////////////////////////////////////////////////////////////////////////////////////////////////
        // SqlDatabase Vendor Specific Configuration
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        switch (dialect) {
            case DEFAULT:
                break;
            case CUBRID:
                break;
            case DERBY:
                break;
            case FIREBIRD:
                break;
            case H2:
                dbPlatform = new H2Platform(configuration, config);
                hikariConfig.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
                hikariConfig.addDataSourceProperty("URL", jdbcUrl);
                hikariConfig.addDataSourceProperty("user", jdbcUser);
                hikariConfig.addDataSourceProperty("password", jdbcPassword);
                hikariReadConfig.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
                hikariReadConfig.addDataSourceProperty("URL", jdbcReadUrl);
                hikariReadConfig.addDataSourceProperty("user", jdbcReadUser);
                hikariReadConfig.addDataSourceProperty("password", jdbcReadPassword);
                break;
            case HSQLDB:
                break;
            case MARIADB:
            case MYSQL:
                dbPlatform = new MySqlPlatform(configuration, config);
                hikariConfig.setUsername(jdbcUser);
                hikariConfig.setPassword(jdbcPassword);
                hikariConfig.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
                hikariConfig.addDataSourceProperty("URL", jdbcUrl);
                hikariConfig.addDataSourceProperty("user", jdbcUser);
                hikariConfig.addDataSourceProperty("password", jdbcPassword);

                hikariReadConfig.setUsername(jdbcReadUser);
                hikariReadConfig.setPassword(jdbcReadPassword);
                hikariReadConfig.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
                hikariReadConfig.addDataSourceProperty("URL", jdbcReadUrl);
                hikariReadConfig.addDataSourceProperty("user", jdbcReadUser);
                hikariReadConfig.addDataSourceProperty("password", jdbcReadPassword);

                ////////////////////////////////////////////////////////////////////////////////////////////////////
                // MySQL Performance Configuration
                ////////////////////////////////////////////////////////////////////////////////////////////////////
                hikariConfig.addDataSourceProperty("cachePrepStmts", config.isCachePrepStmts());
                hikariReadConfig.addDataSourceProperty("cachePrepStmts", config.isCachePrepStmts());
                int prepStmtCacheSize = config.getPrepStmtCacheSize();
                if (prepStmtCacheSize < 1) {
                    prepStmtCacheSize = config.isProd()
                        ? PROD_MYSQL_PREPARE_STMT_CACHE_SIZE
                        : DEV_MYSQL_PREPARE_STMT_CACHE_SIZE;
                }
                hikariConfig.addDataSourceProperty("prepStmtCacheSize", prepStmtCacheSize);
                hikariReadConfig.addDataSourceProperty("prepStmtCacheSize", prepStmtCacheSize);
                int prepStmtCacheSqlLimit = config.getPrepStmtCacheSqlLimit();
                if (prepStmtCacheSqlLimit < 1) {
                    prepStmtCacheSqlLimit = config.isProd()
                        ? PROD_MYSQL_PREPARE_STMT_CACHE_SQL_LIMIT
                        : DEV_MYSQL_PREPARE_STMT_CACHE_SQL_LIMIT;
                }
                hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", prepStmtCacheSqlLimit);
                hikariReadConfig.addDataSourceProperty("prepStmtCacheSqlLimit", prepStmtCacheSqlLimit);
                hikariConfig.addDataSourceProperty("useServerPrepStmts", config.isUseServerPrepStmts());
                hikariReadConfig.addDataSourceProperty("useServerPrepStmts", config.isUseServerPrepStmts());
                break;
            case POSTGRES:
            case POSTGRES_9_3:
            case POSTGRES_9_4:
//                dbPlatform = new PGPlatform(configuration, config);
//                settings.setRenderSchema(true);
//
//                // Manually create PG DataSource.
//                org.postgresql.ds.PGSimpleDataSource d = new PGSimpleDataSource();
//
//                // Manually parse URL.
//                Properties props = org.postgresql.Driver.parseURL(jdbcUrl, new Properties());
//                if (props != null && !props.isEmpty()) {
//                    // Set parsed Properties.
//                    d.setProperty(PGProperty.PG_HOST, PGProperty.PG_HOST.get(props));
//                    d.setProperty(PGProperty.PG_DBNAME, PGProperty.PG_DBNAME.get(props));
//                    d.setProperty(PGProperty.PG_PORT, PGProperty.PG_PORT.get(props));
//                }
//
//                // Set username and password.
//                d.setUser(jdbcUser);
//                d.setPassword(jdbcPassword);
//                d.setCurrentSchema("public");
//                hikariConfig.setDataSource(d);
                break;
            case SQLITE:
                break;
        }

        // Find all Entity classes.
        findEntityClasses();
        // Build jOOQ Schema.
        findJooqSchema();
    }

    public SqlConfig getConfig() {
        return config;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Property Accessors
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @return
     */
    public List<Mapping> getMappings() {
        return Lists.newArrayList(mappings.values());
    }

    public List<TableMapping> getTableMappings() {
        return Collections.unmodifiableList(tableMappingList);
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @return
     */
    public Configuration getConfiguration() {
        return configuration.derive();
    }

    protected HikariDataSource buildWriteDataSource() {
        return new HikariDataSource(hikariConfig);
    }

    protected HikariDataSource buildReadDataSource() {
        return new HikariDataSource(hikariReadConfig);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Start Up
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void startUp() throws Exception {
        writeExecutor = Executors.newFixedThreadPool(config.getMaxPoolSize());
        readExecutor = Executors.newFixedThreadPool(config.getMaxReadPoolSize());

        ////////////////////////////////////////////////////////////////////////////////////////////////////
        // H2 TCP Server to allow remote connections
        ////////////////////////////////////////////////////////////////////////////////////////////////////
//        if (configuration.dialect() == SQLDialect.H2 && AppConfig.get().getDb().getH2Port() > 0) {
//            h2Server = new H2Server(AppConfig.getH2Port());
//            h2Server.startAsync().awaitRunning();
//        }

        try {
            dataSource = buildWriteDataSource();
        } catch (Throwable e) {
            LOG.error("Could not create a Hikari connection pool.", e);
            throw new PersistException(e);
        }

        try {
            readDataSource = buildReadDataSource();
        } catch (Throwable e) {
            LOG.error("Could not create a Hikari read connection pool.", e);
            throw new PersistException(e);
        }

        // Set Connection Provider.
        configuration.set(new DataSourceConnectionProvider(dataSource));

        // Create Read Configuration.
        readConfiguration = configuration.derive(new DataSourceConnectionProvider(readDataSource));

        // Detect changes.
        List<SchemaInspector.Change> changes = buildEvolution();

        // Are there schema changes that need to be applied?
        if (changes == null || !changes.isEmpty()) {
            applyEvolution(changes);

            changes = buildEvolution();

            // Detect changes.
            if (changes != null && !changes.isEmpty()) {
                throw new PersistException("Schema Evolution was applied incompletely.");
            }
        }

        if (config.isGenerateSchema()) {
            return;
        }

        // Finish initializing Table Mappings and ensure Validity.
        // Wire Relationships.
        tableMappingList.forEach(mapping -> mapping.wireRelationships(tableMappings));
        // Validate Table Mapping.
        tableMappingList.forEach(TableMapping::checkValidity);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Shutdown
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void shutDown() throws Exception {
        writeExecutor.shutdown();
        readExecutor.shutdown();

        // Completely destroy H2 memory database if necessary.
        if (config.getUrl().startsWith("jdbc:h2:mem")) {
            Try.run(() -> {
                try (final PreparedStatement stmt = dataSource.getConnection().prepareStatement("DROP ALL OBJECTS;")) {
                    stmt.execute();
                    stmt.close();
                }
            });
        }

        Try.run(() -> dataSource.close())
            .onFailure((e) -> LOG.error("Failed to shutdown Hikari Connection Pool", e));

        Try.run(() -> readDataSource.close())
            .onFailure((e) -> LOG.error("Failed to shutdown Hikari Connection Pool", e));

        // Stop H2 TCP Server if necessary.
        if (h2Server != null) {
            Try.run(() -> h2Server.stopAsync().awaitTerminated())
                .onFailure((e) -> LOG.error("Failed to stop H2 Server", e));
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Initialize
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void buildTableMappings() {
        final Map<Class, TableMapping> mappingMap = Maps.newHashMap();
        for (Class<?> cls : entityClasses) {
            final io.clickhandler.sql.Table tableAnnotation = cls.getAnnotation(io.clickhandler.sql.Table.class);
            if (tableAnnotation == null) {
                continue;
            }
            final TableMapping mapping = TableMapping.create(
                sqlSchema.getTable(TableMapping.tableName(cls, tableAnnotation.name(), false)),
                sqlSchema.getTable(TableMapping.tableName(cls, tableAnnotation.name(), true)),
                dbPlatform,
                cls,
                jooqMap
            );
            mappingMap.put(cls, mapping);
        }

        // Rebind.
        rebindMappings(mappingMap);
    }

    private void rebindMappings(Map<Class, TableMapping> mappings) {
        // Add all mappings.
        this.mappings.clear();
        this.mappings.putAll(mappings);
        this.tableMappingsByEntity.clear();
        this.tableMappingsByEntity.putAll(mappings);
        this.tableMappings.clear();
        this.tableMappings.putAll(mappings);
        this.tableMappingsByName.clear();
        this.tableMappingList.clear();
        this.tableMappingList.addAll(mappings.values());
        final Map<String, TableMapping.Index> indexMap = new HashMap<>();

        // Ensure all Table Mappings are valid.
        for (TableMapping mapping : mappings.values()) {
            for (TableMapping.Index index : mapping.getIndexes()) {
                final TableMapping.Index existingIndex = indexMap.get(index.name);
                if (existingIndex != null) {
                    throw new PersistException("Duplicate Index name [" + index.name + "] was discovered. [" +
                        existingIndex.mapping.entityClass.getCanonicalName() + "] and [" +
                        index.mapping.entityClass.getCanonicalName() + "]");
                }
                indexMap.put(index.name, index);
            }

            if (!config.isGenerateSchema()) {
                final Table tbl = mapping.TBL();

                if (tbl != null) {
                    tableMappings.put(tbl.getClass(), mapping);
                    tableMappings.put(tbl.newRecord().getClass(), mapping);
                    tableMappings.put(tbl.getRecordType(), mapping);
                }
            }

            tableMappingsByName.put(mapping.getTableName(), mapping);
        }
    }

    /**
     * @return
     */
    private void findEntityClasses() {
        // Find all entity classes annotated with @Table
        for (Reflections reflections : entityReflections) {
            final Set<Class<?>> t = reflections.getTypesAnnotatedWith(io.clickhandler.sql.Table.class);
            if (t != null && !t.isEmpty()) {
                entityClasses.addAll(t);
            }
        }
    }

    /**
     *
     */
    private void findJooqSchema() {
        if (config.isGenerateSchema()) {
            return;
        }

        jooqMap.clear();

        for (Reflections reflections : jooqReflections) {
            final Set<Class<? extends TableImpl>> jooqTables = reflections.getSubTypesOf(TableImpl.class);
            for (Class<? extends Table> jooqTableClass : jooqTables) {
                try {
                    final Table jooqTable = jooqTableClass.newInstance();
                    jooqMap.put(Strings.nullToEmpty(jooqTable.getName()).trim().toLowerCase(), jooqTable);
                } catch (Exception e) {
                    throw new PersistException("Failed to instantiate an instance of jOOQ Table Class [" + jooqTableClass.getCanonicalName() + "]");
                }
            }
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Evolution
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Build Evolution Plan.
     *
     * @return
     */
    private List<SchemaInspector.Change> buildEvolution() {
        try (Connection connection = dataSource.getConnection()) {
            this.sqlSchema = new SqlSchema(connection, config.getCatalog(), config.getSchema());
            buildTableMappings();
            return SchemaInspector.inspect(dbPlatform, tableMappingsByEntity);
        } catch (Exception e) {
            // Ignore.
            LOG.error("EvolutionChecker.inspect failed.", e);
            throw new PersistException(e);
        }
    }

    /**
     * Apply the Evolution.
     *
     * @param changes
     */
    private void applyEvolution(final List<SchemaInspector.Change> changes) {
        final EvolutionEntity evolution = new EvolutionEntity();
        final List<EvolutionChangeEntity> changeEntities = new ArrayList<>();

        try {
            executeWrite(session -> {
                evolution.setC(new Date());
                evolution.setStarted(new Date());

                boolean failed = false;

                for (SchemaInspector.Change change : changes) {
                    final String sql = change.ddl(dbPlatform);
                    final String[] sqlParts = sql.trim().split(";");
                    for (String sqlPart : sqlParts) {
                        sqlPart = sqlPart.trim();
                        if (sqlPart.isEmpty()) {
                            continue;
                        }
                        final EvolutionChangeEntity changeEntity = new EvolutionChangeEntity();
                        changeEntities.add(changeEntity);
                        changeEntity.setStarted(new Date());
                        changeEntity.setType(change.type());
                        changeEntity.setSql(sqlPart);
                        try {
                            changeEntity.setAffected((long) session.sqlUpdate(sqlPart));
                            changeEntity.setSuccess(true);
                        } catch (Exception e) {
                            failed = true;
                            LOG.error("SCHEMA CHANGE FAILED: " + sql, e);
                            changeEntity.setMessage(e.getMessage());
                            changeEntity.setSuccess(false);
                        } finally {
                            changeEntity.setEnd(new Date());
                        }
                    }
                }

                evolution.setEnd(new Date());

                if (failed) {
                    throw new PersistException("Failed to update schema.");
                }

                evolution.setSuccess(true);

                // Insert Evolution into Db.
                if (!config.isGenerateSchema()) {
                    session.insert(evolution);
                    session.insert(changeEntities);
                }

                return SqlResult.success(evolution);
            });
        } finally {
            try {
                // Save if evolution failed since the transaction rolled back.
                if (!evolution.isSuccess()) {
                    if (!config.isGenerateSchema()) {
                        // We need to Insert the Evolution outside of the applier
                        // SQL Transaction since it would be rolled back.
                        executeWrite(sql -> {
                            sql.insert(evolution);
                            sql.insert(changeEntities);
                            return SqlResult.success();
                        });
                    }
                }
            } catch (Throwable e) {
                LOG.error("Failed to insert into EVOLUTION table.", e);
                throw new RuntimeException(e);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Execution
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    protected SqlSession createSession(boolean master) {
        final Configuration configuration = master ? this.configuration.derive() : this.readConfiguration.derive();

        return new SqlSession(
            this,
            configuration
        );
    }

    @Override
    public void writeRunnable(SqlRunnable task, Handler<AsyncResult<Void>> handler) {
        writeExecutor.submit(() -> {
            try {
                executeWrite(sql -> {
                    task.run(sql);
                    return null;
                });

                if (handler != null) {
                    vertx.runOnContext(event -> handler.handle(Future.succeededFuture()));
                }
            } catch (Exception e) {
                if (handler != null) {
                    vertx.runOnContext(event -> handler.handle(Future.failedFuture(e)));
                }
            }
        });
    }

    public <T> Observable<SqlResult<T>> write(SqlCallable<T> task) {
        return writeObservable(task);
    }

    public <T> Observable<SqlResult<T>> writeObservable(SqlCallable<T> task) {
        return Observable.create(
            (Observable.OnSubscribe<SqlResult<T>>) subscriber ->
                write(task, result -> {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(result.result());
                        subscriber.onCompleted();
                    }
                }))
            .subscribeOn(observableScheduler)
            .observeOn(observableScheduler);
    }

    /**
     * @param task
     * @param handler
     * @param <T>
     */
    public <T> void write(SqlCallable<T> task, Handler<AsyncResult<SqlResult<T>>> handler) {
        writeExecutor.submit(() -> {
            try {
                final SqlResult<T> result = executeWrite(task);

                if (handler != null) {
                    vertx.runOnContext(event -> handler.handle(Future.succeededFuture(result)));
                }
            } catch (Exception e) {
                if (handler != null) {
                    vertx.runOnContext(event -> handler.handle(Future.failedFuture(e)));
                }
            }
        });
    }

    public <T> Observable<T> read(SqlReadCallable<T> task) {
        return readObservable(task);
    }

    public <T> Observable<T> readObservable(SqlReadCallable<T> task) {
        return Observable.create(
            (Observable.OnSubscribe<T>) subscriber ->
                read(task, result -> {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(result.result());
                        subscriber.onCompleted();
                    }
                }))
            .subscribeOn(observableScheduler)
            .observeOn(observableScheduler);
    }

    /**
     * @param task
     * @param handler
     * @param <T>
     */
    public <T> void read(final SqlReadCallable<T> task, final Handler<AsyncResult<T>> handler) {
        readExecutor.submit(() -> {
            try {
                final T result = executeRead(task);
                if (handler != null) {
                    vertx.runOnContext(event -> handler.handle(Future.succeededFuture(result)));
                }
            } catch (Exception e) {
                if (handler != null) {
                    vertx.runOnContext(event -> handler.handle(Future.failedFuture(e)));
                }
            }
        });
    }

    /**
     * @param task
     * @param <T>
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public <T> T readBlocking(SqlReadCallable<T> task) {
        try {
            return readExecutor.submit(() -> {
                try {
                    return executeRead(task);
                } catch (Exception e) {
                    throw new PersistException(e);
                }
            }).get();
        } catch (InterruptedException e) {
            throw new PersistException(e);
        } catch (ExecutionException e) {
            throw new PersistException(e);
        }
    }

//    /**
//     * @param task
//     * @throws ExecutionException
//     * @throws InterruptedException
//     */
//    public void writeBlocking(SqlRunnable task) {
//        try {
//            writeExecutor.submit(() -> {
//                try {
//                    executeWrite(session -> {
//                        task.run(session);
//                        return null;
//                    });
//                } catch (Exception e) {
//                    throw new PersistException(e);
//                }
//            }).get();
//        } catch (InterruptedException e) {
//            throw new PersistException(e);
//        } catch (ExecutionException e) {
//            throw new PersistException(e);
//        }
//    }

    /**
     * @param task
     * @param <T>
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public <T> SqlResult<T> writeBlocking(SqlCallable<T> task) {
        try {
            return writeExecutor.submit(() -> {
                try {
                    return executeWrite(task);
                } catch (Exception e) {
                    throw new PersistException(e);
                }
            }).get();
        } catch (InterruptedException e) {
            throw new PersistException(e);
        } catch (ExecutionException e) {
            throw new PersistException(e);
        }
    }

    /**
     * @param callable
     * @param <T>
     * @return
     */
    protected <T> T executeRead(SqlReadCallable<T> callable) {
        SqlSession session = sessionLocal.get();

        if (session == null) {
            session = createSession(false);

            if (session == null) {
                throw new PersistException("createSession() returned null.");
            }

            sessionLocal.set(session);
        }

        try {
            return callable.call(session);
        } finally {
            sessionLocal.remove();
        }
    }

    /**
     * @param callable
     * @param <T>
     * @return
     */
    protected <T> SqlResult<T> executeWrite(SqlCallable<T> callable) {
        SqlSession session = sessionLocal.get();
        final boolean created;

        if (session == null) {
            session = createSession(true);

            if (session == null) {
                throw new PersistException("createSession() returned null.");
            }

            sessionLocal.set(session);
            created = true;
        } else {
            created = false;
        }

        final AtomicReference<SqlResult<T>> r = new AtomicReference<>();
        final SqlSession finalSession = session;
        try {
            final AtomicBoolean updateCache = new AtomicBoolean(false);
            try {
                return DSL.using(session.configuration()).transactionResult(configuration1 -> {
                    finalSession.scope(configuration1);

                    try {
                        // Execute the code.
                        final SqlResult<T> result = callable.call(finalSession);
                        r.set(result);

                        // Rollback if ActionResponse isFailure.
                        if (result != null && !result.isSuccess()) {
                            throw new RollbackException();
                        }

                        updateCache.set(true);

                        return result;
                    } finally {
                        finalSession.unscope();
                    }
                });
            } finally {
                if (created) {
                    sessionLocal.remove();
                }

                // Update cache.
                if (updateCache.get()) {
                    Try.run(() -> finalSession.updateCache());
                }
            }
        } catch (RollbackException e) {
            return r.get();
        }
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Mappings
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @param cls
     * @return
     */
    Mapping findMapping(Class cls) {
        Mapping mapping = mappings.get(cls);

        if (mapping == null) {
            synchronized (SqlDatabase.class) {
                mapping = mappings.get(cls);

                if (mapping == null) {
                    try {
                        mapping = new Mapping(cls, dbPlatform);
                        mappings.put(cls, mapping);
                    } catch (Throwable e) {
                        LOG.error("Failed to create Mapping for [" + cls.getCanonicalName() + "]", e);
                        return null;
                    }
                }
            }
        }

        return mapping;
    }

    public TableMapping getMapping(Class cls) {
        return tableMappings.get(cls);
    }

    public <E extends AbstractEntity> TableMapping getMapping(E entity) {
        Preconditions.checkNotNull(entity);
        return tableMappings.get(entity.getClass());
    }

    public <R extends Record> TableMapping getMapping(R record) {
        Preconditions.checkNotNull(record);
        return tableMappings.get(record.getClass());
    }

    public <T extends Table> TableMapping getMapping(T table) {
        return tableMappings.get(table.getClass());
    }

    /**
     * @param entityClass
     * @param <E>
     * @return
     */
    <E extends AbstractEntity> E entity(Class<E> entityClass) {
        final TableMapping mapping = getMapping(entityClass);
        if (mapping == null) {
            return null;
        }
        try {
            return (E) mapping.fastClass.newInstance();
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> E copy(E entity) {
        final TableMapping mapping = getMapping(entity.getClass());
        if (mapping == null) {
            return null;
        }
        try {
            return mapping.copy(entity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param cls
     * @return
     */
    TableMapping getCheckedMapping(Class cls) {
        Preconditions.checkNotNull(cls, "cls must be specified.");
        final TableMapping mapping = getMapping(cls);
        if (mapping == null) {
            throw new PersistException("Mapping for class [" + cls.getCanonicalName() + "] was not found.");
        }
        return mapping;
    }

    /**
     * @param mapping
     * @param <R>
     * @param <E>
     * @return
     */
    <R extends Record, E extends AbstractEntity> RecordMapper<R, E> recordMapper(TableMapping mapping) {
        return (RecordMapper<R, E>) mapping.getRecordMapper();
    }

    /**
     * @param mapping
     * @return
     */
    RecordMapper<Record, Record> journalMapper(TableMapping mapping) {
        return mapping.getJournalMapper();
    }

    /**
     * @param cls
     * @param <R>
     * @param <E>
     * @return
     */
    <R extends Record, E extends AbstractEntity> RecordMapper<R, E> recordMapper(Class cls) {
        return (RecordMapper<R, E>) getCheckedMapping(cls).getRecordMapper();
    }

    /**
     * @param cls
     * @param <R>
     * @param <E>
     * @return
     */
    <R extends Record, E extends AbstractEntity> RecordMapper<R, E> mapper(Class cls) {
        return (RecordMapper<R, E>) getCheckedMapping(cls).getRecordMapper();
    }

    /**
     * @param cls
     * @param <E>
     * @param <R>
     * @return
     */
    <E extends AbstractEntity, R extends Record> EntityMapper<E, R> entityMapper(Class cls) {
        return (EntityMapper<E, R>) getCheckedMapping(cls).getEntityMapper();
    }

    /**
     * @param list
     * @param <E>
     * @param <R>
     * @return
     */
    <E extends AbstractEntity, R extends Record> EntityMapper<E, R> entityMapper(List list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return (EntityMapper<E, R>) getCheckedMapping(list.get(0).getClass()).getEntityMapper();
    }

    /**
     * @param record
     * @param <R>
     * @param <E>
     * @return
     */
    <R extends Record, E extends AbstractEntity> E map(R record) {
        if (record == null) {
            return null;
        }
        return ((RecordMapper<R, E>) mapper(record.getClass())).map(record);
    }

    /**
     * @param entity
     * @param <E>
     * @param <R>
     * @return
     */
    <E extends AbstractEntity, R extends Record> R map(E entity) {
        if (entity == null) {
            return null;
        }
        return (R) entityMapper((Class<E>) entity.getClass()).map(entity);
    }

    /**
     * @param records
     * @param <R>
     * @param <E>
     * @return
     */
    <R extends Record, E extends AbstractEntity> List<E> map(R... records) {
        if (records == null) {
            return new ArrayList<>(0);
        }
        return map(Lists.newArrayList(records));
    }

    /**
     * @param records
     * @param <R>
     * @param <E>
     * @return
     */
    <R extends Record, E extends AbstractEntity> List<E> map(Collection<R> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>(0);
        }

        final List<E> entities = Lists.newArrayListWithCapacity(records.size());
        RecordMapper<R, E> mapper = null;
        for (R record : records) {
            if (mapper == null) {
                mapper = mapper((Class<R>) record.getClass());
            }
            entities.add(mapper.map(record));
        }
        return entities;
    }

    public Configuration getReadConfiguration() {
        return readConfiguration;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // jOOQ Impl
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     */
    public static class H2Server extends AbstractIdleService {
        private final int port;
        private Server server;
        private TcpServer tcpServer;

        public H2Server(int port) {
            this.port = port;
        }

        @Override
        protected void startUp() throws Exception {
            tcpServer = new TcpServer();
            tcpServer.init("-tcpPort", Integer.toString(port));
            server = new Server(tcpServer);
            server.start();
        }

        @Override
        protected void shutDown() throws Exception {
            try {
                server.stop();
            } finally {
                try {
                    tcpServer.stop();
                } catch (Exception e) {
                    // Ignore.
                }
            }
        }
    }

    public static class PrettyPrinter extends DefaultExecuteListener {
        public static Logger LOG = LoggerFactory.getLogger(PrettyPrinter.class);

        /**
         * Hook into the query execution lifecycle before executing queries
         */
        @Override
        public void executeStart(ExecuteContext ctx) {
            if (!LOG.isDebugEnabled())
                return;

            // Create a new DSLContext for logging rendering purposes
            // This DSLContext doesn't need a connection, only the SQLDialect...
            DSLContext create = DSL.using(ctx.dialect(),

                // ... and the flag for pretty-printing
                new Settings().withRenderFormatted(true));

            // If we're executing a query
            if (ctx.query() != null) {
                LOG.debug(create.renderInlined(ctx.query()));
            }

            // If we're executing a routine
            else if (ctx.routine() != null) {
                LOG.debug(create.renderInlined(ctx.routine()));
            }

            // If we're executing anything else (e.g. plain SQL)
            else if (!StringUtils.isBlank(ctx.sql())) {
                LOG.debug(ctx.sql());
            }
        }
    }

    public class DeleteOrUpdateWithoutWhereListener extends DefaultExecuteListener {

        @Override
        public void renderEnd(ExecuteContext ctx) {
            if (ctx.sql().matches("^(?i:(UPDATE|DELETE)(?!.* WHERE ).*)$")) {
                throw new DeleteOrUpdateWithoutWhereException();
            }
        }
    }

    public class DeleteOrUpdateWithoutWhereException extends RuntimeException {
    }

    /**
     *
     */
    private final class RecordMapperProviderImpl implements RecordMapperProvider {
        @Override
        public <R extends Record, E> RecordMapper<R, E> provide(RecordType<R> recordType, Class<? extends E> type) {
            final TableMapping mapping = tableMappings.get(type);
            if (mapping == null) {
                return null;
            }
            return (RecordMapper<R, E>) mapping.getRecordMapper();
        }
    }
}
