package move.sql;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.Service;
import com.nuodb.jdbc.RemConnection;
import com.nuodb.jdbc.RemStatement;
import com.nuodb.jdbc.SQLException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rxjava.core.Vertx;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javaslang.Tuple2;
import javaslang.control.Try;
import javax.sql.DataSource;
import move.action.ActionTimeoutException;
import move.action.JobAction;
import move.action.MoveEventLoop;
import move.action.MoveKernel;
import move.common.UID;
import move.metrics.Metrics;
import move.threading.WorkerPool;
import org.h2.server.TcpServer;
import org.h2.tools.Server;
import org.jooq.Condition;
import org.jooq.Configuration;
import org.jooq.ConnectionCallable;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.ExecuteContext;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.RecordMapperProvider;
import org.jooq.RecordType;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.conf.BackslashEscaping;
import org.jooq.conf.ParamType;
import org.jooq.conf.RenderKeywordStyle;
import org.jooq.conf.RenderNameStyle;
import org.jooq.conf.Settings;
import org.jooq.conf.StatementType;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultConnectionProvider;
import org.jooq.impl.DefaultExecuteListener;
import org.jooq.impl.DefaultRecordMapperProvider;
import org.jooq.impl.TableImpl;
import org.jooq.impl.ThreadLocalTransactionProvider;
import org.jooq.tools.StringUtils;
import org.jooq.tools.jdbc.JDBCUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Single;

/**
 * @author Clay Molocznik
 */
@SuppressWarnings("all")
public class SqlDatabase extends AbstractIdleService {

  private static final int SANE_MAX = 250;
  private static final int TIMEOUT_THRESHOLD = 150;
  private static final int MAX_WRITE_TASKS = 2000;
  private static final int MAX_READ_TASKS = 2000;
  private static final int MAX_QUEUE_CAPACITY = 200000;
  private static final int MINIMUM_TIMEOUT = 2000;
  private static final int DEV_POOL_SIZE = 15;
  private static final int TEST_POOL_SIZE = 15;
  private static final int PROD_POOL_SIZE = 100;
  private static final int PROD_READ_POOL_SIZE = 100;
  private static final int PROD_MYSQL_PREPARE_STMT_CACHE_SIZE = 4096;
  private static final int DEV_MYSQL_PREPARE_STMT_CACHE_SIZE = 256;
  private static final int PROD_MYSQL_PREPARE_STMT_CACHE_SQL_LIMIT = 4096;
  private static final int DEV_MYSQL_PREPARE_STMT_CACHE_SQL_LIMIT = 2048;
  private static final String ENTITY_PACKAGE = "move.sql";
  private static final Logger LOG = LoggerFactory.getLogger(SqlDatabase.class);
  private static final String ACTION_KEY = "a";

  protected final SqlConfig config;
  protected final String name;
  protected final Set<Class<?>> entityClasses = new HashSet<>();
  protected final Vertx vertx;
  private final Map<Class, TableMapping> mappings = new HashMap<>();
  private final Map<String, TableMapping> tableMappingsByName = Maps.newHashMap();
  private final Map<Class, TableMapping> tableMappings = Maps.newHashMap();
  private final Map<Class, TableMapping> tableMappingsByEntity = Maps.newHashMap();
  private final ConcurrentHashMap<String, Timer> readTimerMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Timer> writeTimerMap = new ConcurrentHashMap<>();
  private final List<TableMapping> tableMappingList = Lists.newArrayList();
  private final String[] entityPackageNames;
  private final String[] jooqPackageNames;
  private final Map<String, Table> jooqMap = Maps.newHashMap();
  private final Reflections[] entityReflections;
  private final int readTaskTimout;
  private final int writeTaskTimout;
  private final SQLDialect dialect;
  private final boolean isNuoDB;
  private final LinkedBlockingDeque<Tuple2<Statement, Boolean>> rogueStatementQueue = new LinkedBlockingDeque<>();
  //    protected HikariConfig hikariConfig;
//    protected HikariConfig hikariReadConfig;
  protected Configuration configuration;
  protected Configuration readConfiguration;
  protected DataSource dataSource;
  protected DataSource readDataSource;
  protected SqlSchema sqlSchema;
  protected SqlPlatform dbPlatform;
  protected Settings settings;
  protected H2Server h2Server;
  private Service statementCleanupService;
  private Timer readTimer;
  private Timer writeTimer;
  private Counter readTimeoutsCounter;
  private Counter readExceptionsCounter;
  private Counter writeTimeoutsCounter;
  private Counter writeExceptionsCounter;
  private Counter rogueStatementsCounter;
  private Counter rogueWriteStatementsCounter;
  private Counter rogueReadStatementsCounter;
  private Counter rogueStatementExceptionsCounter;

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // Property Accessors
  ////////////////////////////////////////////////////////////////////////////////////////////////////
  private MetricRegistry metricRegistry;

  private WorkerPool readExecutor;
  private WorkerPool writeExecutor;

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // Start Up
  ////////////////////////////////////////////////////////////////////////////////////////////////////

  public SqlDatabase(
      Vertx vertx,
      SqlConfig config,
      String[] entityPackageNames,
      String[] jooqPackageNames) {
    entityPackageNames = entityPackageNames == null
        ? new String[0]
        : entityPackageNames;
    jooqPackageNames = jooqPackageNames == null
        ? new String[0]
        : jooqPackageNames;
    this.vertx = vertx;
    this.config = Preconditions.checkNotNull(config, "config must be set.");
    this.name = Strings.nullToEmpty(config.getName()).trim();
    this.entityPackageNames = entityPackageNames;
    this.jooqPackageNames = jooqPackageNames;

    this.metricRegistry = Metrics.registry();

    this.isNuoDB = config.getUrl().startsWith("jdbc:com.nuodb");
    this.dialect = isNuoDB
        ? SQLDialect.MYSQL
        : JDBCUtils.dialect(Strings.nullToEmpty(config.getUrl()));

    if (config.getReadTaskTimeout() < MINIMUM_TIMEOUT) {
      this.readTaskTimout = MINIMUM_TIMEOUT;
    } else {
      this.readTaskTimout = config.getReadTaskTimeout();
    }

    if (config.getWriteTaskTimeout() < MINIMUM_TIMEOUT) {
      this.writeTaskTimout = MINIMUM_TIMEOUT;
    } else {
      this.writeTaskTimout = config.getWriteTaskTimeout();
    }

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

      // Add Core Entity path.
      entityReflections.add(new Reflections(ENTITY_PACKAGE));
    }

    this.entityReflections = entityReflections.toArray(new Reflections[entityReflections.size()]);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // Shutdown
  ////////////////////////////////////////////////////////////////////////////////////////////////////

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    final ExecutorService executorService = Executors.newFixedThreadPool(4);

    for (int i = 0; i < 100; i++) {
      vertx.runOnContext(action -> {
        final io.vertx.rxjava.core.Context context = vertx.getOrCreateContext();

        Observable.<List<String>>create(subscriber -> {
          final String name1 = Thread.currentThread().getName();
          vertx.setTimer(1, a2 -> {
            final String name2 = Thread.currentThread().getName();

            vertx.setTimer(1, a -> {
              executorService.submit(() -> {
                context.runOnContext($ -> {
                  subscriber
                      .onNext(Lists.newArrayList(name1, name2, Thread.currentThread().getName()));
                });
              });
            });
          });
        }).subscribe(
            r -> {
              synchronized (SqlDatabase.class) {
                r.forEach(System.out::println);
                System.out.println(Thread.currentThread().getName());
                System.out.println();
              }
            }
        );
      });
    }
  }

  /**
   * @return
   */
  public SqlConfig getConfig() {
    return config;
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

  protected com.nuodb.jdbc.DataSource buildNuoDB(boolean readOnly, int maxConnections) {
    final String jdbcUrl = Strings.nullToEmpty(readOnly
        ? config.getReadUrl()
        : config.getUrl()).trim();
    final String jdbcUser = Strings.nullToEmpty(readOnly
        ? config.getReadUser()
        : config.getUser()).trim();
    final String jdbcPassword = Strings.nullToEmpty(readOnly
        ? config.getReadPassword()
        : config.getPassword()).trim();

    final Properties properties = new Properties();
    properties.setProperty(com.nuodb.jdbc.DataSource.PROP_URL, jdbcUrl);
    properties.setProperty(com.nuodb.jdbc.DataSource.PROP_USERNAME, jdbcUser);
    properties.setProperty(com.nuodb.jdbc.DataSource.PROP_PASSWORD, jdbcPassword);

    if (config.getSchema() != null && !config.getSchema().trim().isEmpty()) {
      properties.setProperty(com.nuodb.jdbc.DataSource.PROP_SCHEMA, config.getSchema().trim());
    }

    properties.setProperty(com.nuodb.jdbc.DataSource.PROP_DEFAULTREADONLY,
        Boolean.toString(readOnly && maxConnections == 0));
    properties.setProperty(com.nuodb.jdbc.DataSource.PROP_DEFAULTAUTOCOMMIT,
        Boolean.toString(readOnly && maxConnections == 0));

    int maximumPoolSize = maxConnections;

    if (maximumPoolSize == 0) {
      // Set the default maximum pool size.
      if (config.isDev()) {
        maximumPoolSize = Runtime.getRuntime().availableProcessors();
      } else if (config.isTest()) {
        maximumPoolSize = Runtime.getRuntime().availableProcessors();
      } else {
        maximumPoolSize = Runtime.getRuntime().availableProcessors() * 3;
      }

      // Sanitize Max Pool size.
      if (!readOnly) {
        if (config.getMaxPoolSize() > 0 && config.getMaxPoolSize() <= SANE_MAX) {
          maximumPoolSize = config.getMaxPoolSize();
        } else {
          LOG.warn("An Invalid MaxPoolSize value was found. Found '" + config.getMaxPoolSize()
              + "' but set to a more reasonable '" + SANE_MAX + "'");
          maximumPoolSize = SANE_MAX;
        }
      } else {
        if (config.getMaxReadPoolSize() > 0 && config.getMaxReadPoolSize() < SANE_MAX) {
          maximumPoolSize = config.getMaxReadPoolSize();
        } else {
          LOG.warn("An Invalid MaxReadPoolSize value was found. Found '" + config.getMaxPoolSize()
              + "' but set to a more reasonable '" + SANE_MAX + "'");
          maximumPoolSize = SANE_MAX;
        }
      }
    }

    int initialSize = maximumPoolSize;
    if (!config.isProd()) {
      initialSize = 0;
    }

    properties
        .setProperty(com.nuodb.jdbc.DataSource.PROP_INITIALSIZE, Integer.toString(initialSize));
    properties.setProperty(com.nuodb.jdbc.DataSource.PROP_MINIDLE, Integer.toString(initialSize));
    properties
        .setProperty(com.nuodb.jdbc.DataSource.PROP_MAXACTIVE, Integer.toString(maximumPoolSize));
    properties
        .setProperty(com.nuodb.jdbc.DataSource.PROP_MAXIDLE, Integer.toString(maximumPoolSize));

    properties.setProperty(com.nuodb.jdbc.DataSource.PROP_VALIDATIONQUERY, "SELECT 1 FROM DUAL");
    properties.setProperty(com.nuodb.jdbc.DataSource.PROP_IDLEVALIDATIONINTERVAL, "5000");

    return new com.nuodb.jdbc.DataSource(properties);
  }

  protected HikariDataSource buildHikari(boolean readOnly) {
    final String jdbcUrl = Strings.nullToEmpty(readOnly
        ? config.getReadUrl()
        : config.getUrl()).trim();
    final String jdbcUser = Strings.nullToEmpty(readOnly
        ? config.getReadUser()
        : config.getUser()).trim();
    final String jdbcPassword = Strings.nullToEmpty(readOnly
        ? config.getReadPassword()
        : config.getPassword()).trim();

    final HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setAutoCommit(readOnly);
    hikariConfig.setReadOnly(readOnly);

    if (config.getLeakDetectionThreshold() > 0) {
      hikariConfig.setLeakDetectionThreshold(config.getLeakDetectionThreshold());
    }

    // Set the default maximum pool size.
    if (config.isDev()) {
      hikariConfig.setMaximumPoolSize(Runtime.getRuntime().availableProcessors());
    } else if (config.isTest()) {
      hikariConfig.setMaximumPoolSize(Runtime.getRuntime().availableProcessors());
    } else {
      hikariConfig.setMaximumPoolSize(Runtime.getRuntime().availableProcessors() * 3);
    }

    // Sanitize Max Pool size.
    if (!readOnly) {
      if (config.getMaxPoolSize() > 0 && config.getMaxPoolSize() <= SANE_MAX) {
        hikariConfig.setMaximumPoolSize(config.getMaxPoolSize());
      } else {
        LOG.warn("An Invalid MaxPoolSize value was found. Found '" + config.getMaxPoolSize()
            + "' but set to a more reasonable '" + SANE_MAX + "'");
        hikariConfig.setMaximumPoolSize(SANE_MAX);
      }
    } else {
      if (config.getMaxReadPoolSize() > 0 && config.getMaxReadPoolSize() < SANE_MAX) {
        hikariConfig.setMaximumPoolSize(config.getMaxReadPoolSize());
      } else {
        LOG.warn("An Invalid MaxReadPoolSize value was found. Found '" + config.getMaxPoolSize()
            + "' but set to a more reasonable '" + SANE_MAX + "'");
        hikariConfig.setMaximumPoolSize(SANE_MAX);
      }
    }

    // Always use READ_COMMITTED.
    // Persist depends on this isolation level.
    hikariConfig.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
    // Set Connection Test Query.
    // This is valid SQL and is supported by any "ACTUAL" SQL database engine.
    hikariConfig.setConnectionTestQuery("SELECT 1");
    hikariConfig.setValidationTimeout(5000);
    hikariConfig.setPoolName(("sql-" + config.getName() + "-" + (readOnly
        ? "write"
        : "read")).toLowerCase());
    hikariConfig.setMetricRegistry(metricRegistry);

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
        hikariConfig.setDataSourceClassName("org.h2.jdbcx.JdbcDataSource");
        hikariConfig.addDataSourceProperty("URL", jdbcUrl);
        hikariConfig.addDataSourceProperty("user", jdbcUser);
        hikariConfig.addDataSourceProperty("password", jdbcPassword);
        break;
      case HSQLDB:
        break;
      case MARIADB:
      case MYSQL:
        hikariConfig.setUsername(jdbcUser);
        hikariConfig.setPassword(jdbcPassword);
        hikariConfig.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        hikariConfig.addDataSourceProperty("URL", jdbcUrl);
        hikariConfig.addDataSourceProperty("user", jdbcUser);
        hikariConfig.addDataSourceProperty("password", jdbcPassword);

        ////////////////////////////////////////////////////////////////////////////////////////////////////
        // MySQL Performance Configuration
        ////////////////////////////////////////////////////////////////////////////////////////////////////
        hikariConfig.addDataSourceProperty("cachePrepStmts", config.isCachePrepStmts());
        int prepStmtCacheSize = config.getPrepStmtCacheSize();
        if (prepStmtCacheSize < 1) {
          prepStmtCacheSize = config.isProd()
              ? PROD_MYSQL_PREPARE_STMT_CACHE_SIZE
              : DEV_MYSQL_PREPARE_STMT_CACHE_SIZE;
        }
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", prepStmtCacheSize);
        int prepStmtCacheSqlLimit = config.getPrepStmtCacheSqlLimit();
        if (prepStmtCacheSqlLimit < 1) {
          prepStmtCacheSqlLimit = config.isProd()
              ? PROD_MYSQL_PREPARE_STMT_CACHE_SQL_LIMIT
              : DEV_MYSQL_PREPARE_STMT_CACHE_SQL_LIMIT;
        }
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", prepStmtCacheSqlLimit);
        hikariConfig.addDataSourceProperty("useServerPrepStmts", config.isUseServerPrepStmts());
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

    return new HikariDataSource(hikariConfig);
  }


  protected void startUp() throws Exception {
    try {
      if (config.isProd()) {
        readExecutor = WorkerPool.create(
            vertx,
            "sql-" + config.getName() + "-read",
            config.getReadThreads() < 1
                ? 1
                : config.getReadThreads() > 2048
                    ? 2048
                    : config.getReadThreads(),
            TimeUnit.MINUTES.toNanos(60)
        );
      } else {
        readExecutor = WorkerPool.global(vertx);
      }

      if (config.isProd()) {
        writeExecutor = WorkerPool.create(
            vertx,
            "sql-" + config.getName() + "-write",
            config.getWriteThreads() < 1
                ? 1
                : config.getWriteThreads() > 2048
                    ? 2048
                    : config.getWriteThreads(),
            TimeUnit.MINUTES.toNanos(60)
        );
      } else {
        writeExecutor = WorkerPool.global(vertx);
      }

      final long started = System.currentTimeMillis();
      LOG.info("Starting SqlDatabase...");

      readTimer = metricRegistry.timer("SQL-" + config.getName() + "-READ");
      writeTimer = metricRegistry.timer("SQL-" + config.getName() + "-WRITE");
      readTimeoutsCounter = metricRegistry.counter("SQL-" + config.getName() + "-READ_TIMEOUTS");
      writeTimeoutsCounter = metricRegistry.counter("SQL-" + config.getName() + "-WRITE_TIMEOUTS");
      readExceptionsCounter = metricRegistry
          .counter("SQL-" + config.getName() + "-READ_EXCEPTIONS");
      writeExceptionsCounter = metricRegistry
          .counter("SQL-" + config.getName() + "-WRITE_EXCEPTIONS");
      rogueStatementsCounter = metricRegistry.counter("SQL-" + config.getName() + "-ROGUES");
      rogueWriteStatementsCounter = metricRegistry
          .counter("SQL-" + config.getName() + "-ROGUE_WRITES");
      rogueReadStatementsCounter = metricRegistry
          .counter("SQL-" + config.getName() + "-ROGUE_READS");

      // Configure jOOQ settings.
      settings = new Settings();
      settings.setRenderSchema(false);
      settings.setExecuteWithOptimisticLocking(false);
      settings.setRenderNameStyle(RenderNameStyle.QUOTED);
      settings.setRenderKeywordStyle(RenderKeywordStyle.UPPER);
      settings.setReflectionCaching(true);
      settings.setParamType(ParamType.INDEXED);
      settings.setAttachRecords(true);
      settings.setUpdatablePrimaryKeys(false);
      if (!isNuoDB) {
        settings.setQueryTimeout(config.getDefaultQueryTimeoutInSeconds());
      }
      settings.setBackslashEscaping(BackslashEscaping.DEFAULT);
      settings.setStatementType(StatementType.PREPARED_STATEMENT);

      // Init jOOQ Configuration.
      configuration = new DefaultConfiguration();
      configuration.set(new RecordMapperProviderImpl());

      configuration.set(settings);

      configuration.set(PrettyPrinter::new, TimeoutListener::new);

      if (isNuoDB) {
        // Use MySQL Dialect.
        configuration.set(SQLDialect.MYSQL);
        dbPlatform = new NuoDBPlatform(configuration, config);
      } else {
        configuration.set(dialect);

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
            break;
          case HSQLDB:
            break;
          case MARIADB:
          case MYSQL:
            dbPlatform = config.isMemSQL()
                ? new MemSqlPlatform(configuration, config)
                : new MySqlPlatform(configuration, config);
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
      }

      // Find all Entity classes.
      findEntityClasses();
      // Build jOOQ Schema.
      findJooqSchema();

      ////////////////////////////////////////////////////////////////////////////////////////////////////
      // H2 TCP Server to allow remote connections
      ////////////////////////////////////////////////////////////////////////////////////////////////////
//        if (configuration.dialect() == SQLDialect.H2 && AppConfig.get().getDb().getH2Port() > 0) {
//            h2Server = new H2Server(AppConfig.getH2Port());
//            h2Server.startAsync().awaitRunning();
//        }

      try {
        if (isNuoDB) {
          dataSource = buildNuoDB(false, 0);
          statementCleanupService = new NuoDBStatementCleaner(
              buildNuoDB(false, 1),
              buildNuoDB(true, 1)
          );
        } else {
          dataSource = buildHikari(false);
          statementCleanupService = new StatementCleaner();
        }
      } catch (Throwable e) {
        LOG.error("Could not create a Hikari connection pool.", e);
        throw new PersistException(e);
      }

      if (!Strings.nullToEmpty(config.getReadUrl()).trim().isEmpty()) {
        try {
          if (isNuoDB) {
            readDataSource = buildNuoDB(true, 0);
          } else {
            readDataSource = buildHikari(true);
          }
        } catch (Throwable e) {
          LOG.error("Could not create a Hikari read connection pool.", e);
          throw new PersistException(e);
        }

        readConfiguration = configuration.derive();
      } else {
        readDataSource = dataSource;
        readConfiguration = configuration;
      }

      if (config.isEvolutionEnabled()) {
        // Detect changes.
        List<SchemaInspector.Change> changes = buildEvolution();

        // Are there schema changes that need to be applied?
        if (changes != null && !changes.isEmpty()) {
          if (config.isProd()) {
            throw new PersistException(
                "Schema is not in sync and 'prod' mode does not allow evolutions");
          }

          // Evolve the schema.
          applyEvolution(changes);

          // Check if we are in sync now.
          changes = buildEvolution();

          // Detect changes.
          if (changes != null && !changes.isEmpty()) {
            throw new PersistException("Schema Evolution was applied incompletely.");
          }
        }
      } else {
        buildTableMappings();
      }

      if (config.isGenerateSchema()) {
        return;
      }

      if (config.isEvolutionEnabled()) {
        // Finish initializing Table Mappings and atomic Validity.
        // Validate Table Mapping.
        tableMappingList.forEach(TableMapping::checkValidity);
      }

      statementCleanupService.startAsync().awaitRunning();

      LOG.info("Finished starting SqlDatabase in " + (System.currentTimeMillis() - started) + "ms");
    } catch (Throwable e) {
      LOG.error("Failed to start", e);
//            Try.run(this::stopAsync);
      throw new RuntimeException(e);
    }
  }

  private int ensureThreads(int threads, int poolSize) {
    if (threads == 0) {
      threads = (int) (poolSize * 0.9) + 1;
    }
    if (threads <= 5) {
      threads = 5;
    }
    return threads;
  }

  protected void shutDown() throws Exception {
    // Completely destroy H2 memory database if necessary.
    if (config.getUrl().startsWith("jdbc:h2:mem")) {
      Try.run(() -> {
        try (final PreparedStatement stmt = dataSource.getConnection()
            .prepareStatement("DROP ALL OBJECTS;")) {
          stmt.execute();
          stmt.close();
        }
      });
    }

    Try.run(() -> statementCleanupService.stopAsync().awaitTerminated())
        .onFailure((e) -> LOG
            .error("Failed to stop " + statementCleanupService.getClass().getCanonicalName(), e));

    if (dataSource instanceof HikariDataSource) {
      Try.run(() -> ((HikariDataSource) dataSource).close())
          .onFailure((e) -> LOG.error("Failed to shutdown Hikari Connection Pool", e));
    }

    if (readDataSource instanceof HikariDataSource) {
      Try.run(() -> ((HikariDataSource) readDataSource).close())
          .onFailure((e) -> LOG.error("Failed to shutdown Hikari Connection Pool", e));
    }

    Try.run(() -> readExecutor.close())
        .onFailure(e -> LOG.error("readExecutor.close() threw an exception", e));
    Try.run(() -> writeExecutor.close())
        .onFailure(e -> LOG.error("writeExecutor.close() threw an exception", e));

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
      final move.sql.Table tableAnnotation = cls.getAnnotation(move.sql.Table.class);
      if (tableAnnotation == null) {
        continue;
      }
      final TableMapping mapping = TableMapping.create(
          sqlSchema != null
              ? sqlSchema.getTable(SqlUtils.tableName(cls, tableAnnotation.name()))
              : null,
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
      final Set<Class<?>> t = reflections.getTypesAnnotatedWith(move.sql.Table.class);
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

    for (String jooqPackage : jooqPackageNames) {
      try {
        Class cls = Class.forName(jooqPackage + ".Tables");
        java.lang.reflect.Field[] fields = cls.getDeclaredFields();
        Arrays.stream(fields).forEach(field -> {
          if (TableImpl.class.isAssignableFrom(field.getType())) {
            try {
              final String name = ((Table) field.getType().newInstance()).getName();
              final Table jooqTable = (Table) field.get(field.getType());
              jooqMap.put(Strings.nullToEmpty(name).trim().toLowerCase(), jooqTable);
            } catch (Exception e) {
              throw new PersistException(
                  "Failed to instantiate an instance of jOOQ Table Class [" + field.getType()
                      .getCanonicalName() + "]");
            }
          }
        });
      } catch (Throwable e) {
        Throwables.propagate(e);
      }
    }

//        for (Reflections reflections : jooqReflections) {
//            final Set<Class<? extends TableImpl>> jooqTables = reflections.getSubTypesOf(TableImpl.class);
//            for (Class<? extends Table> jooqTableClass : jooqTables) {
//                try {
//                    final Table jooqTable = jooqTableClass.newInstance();
//                    jooqMap.put(Strings.nullToEmpty(jooqTable.getName()).trim().toLowerCase(), jooqTable);
//                } catch (Exception e) {
//                    throw new PersistException("Failed to instantiate an instance of jOOQ Table Class [" + jooqTableClass.getCanonicalName() + "]");
//                }
//            }
//        }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // Evolution
  ////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Build Evolution Plan.
   */
  private List<SchemaInspector.Change> buildEvolution() {
    try (Connection connection = dataSource.getConnection()) {
      this.sqlSchema = new SqlSchema(connection, config.getCatalog(), config.getSchema(),
          config.isSyncIndexes());
      buildTableMappings();
      return SchemaInspector.inspect(dbPlatform, tableMappingsByEntity, config.isSyncIndexes(),
          config.isDropColumns(), config.getAdvisorFile());
    } catch (Exception e) {
      // Ignore.
      LOG.error("EvolutionChecker.inspect failed.", e);
      throw new PersistException(e);
    }
  }

  /**
   * Apply the Evolution.
   */
  private void applyEvolution(final List<SchemaInspector.Change> changes) {
    final EvolutionEntity evolution = new EvolutionEntity();
    evolution.setId(UID.next());
    final List<EvolutionChangeEntity> changeEntities = new ArrayList<>();

    try {
      final SqlAction action = new SqlAction(null, Long.MAX_VALUE);
      executeWrite(action, session -> {
        evolution.setStarted(LocalDateTime.now());

        boolean failed = false;

        for (SchemaInspector.Change change : changes) {
          final String sql = change.ddl(dbPlatform);
          if (sql == null || sql.isEmpty()) {
            continue;
          }

          final String[] sqlParts = sql.trim().split(";");
          for (String sqlPart : sqlParts) {
            sqlPart = sqlPart.trim();
            if (sqlPart.isEmpty()) {
              continue;
            }
            final EvolutionChangeEntity changeEntity = new EvolutionChangeEntity();
            changeEntity.setId(UUID.randomUUID().toString().replace("-", ""));
            changeEntities.add(changeEntity);
            changeEntity.setStarted(LocalDateTime.now());
            changeEntity.setType(change.type());
            changeEntity.setSql(sqlPart);
            try {
              changeEntity.setAffected(session.create().query(sqlPart).queryTimeout(600).execute());
              changeEntity.setSuccess(true);
            } catch (Exception e) {
              failed = true;
              LOG.error("SCHEMA CHANGE FAILED: " + sql, e);
              changeEntity.setMessage(e.getMessage());
              changeEntity.setSuccess(false);
            } finally {
              changeEntity.setEnd(LocalDateTime.now());
            }
          }
        }

        evolution.setEnd(LocalDateTime.now());

        if (failed) {
          throw new PersistException("Failed to create schema.");
        }

        evolution.setSuccess(true);

        // Insert Evolution into Db.
        if (!config.isGenerateSchema()) {
          session.insert(evolution);
          changeEntities.forEach(session::insert);
        }

        return move.sql.SqlResult.success(evolution);
      });
    } catch (Throwable e) {
      LOG.error("Failed to insert into EVOLUTION table.", e);
      throw new RuntimeException(e);
    } finally {
      try {
        // Save if evolution failed since the transaction rolled back.
        if (!evolution.isSuccess()) {
          if (!config.isGenerateSchema()) {
            final SqlAction action = new SqlAction(null, Long.MAX_VALUE);
            // We need to Insert the Evolution outside of the applier
            // SQL Transaction since it would be rolled back.
            executeWrite(action, sql -> {
              sql.insert(evolution);
              sql.insert(changeEntities);
              return move.sql.SqlResult.success();
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

  /**
   * @param entityClass
   * @param id
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<T> get(Class<T> entityClass, String id) {
    if (id == null || id.isEmpty()) {
      return Single.just(null);
    }
    return read(sql -> sql.getEntity(entityClass, id));
  }

  /**
   * @param entityClass
   * @param ids
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<List<T>> get(Class<T> entityClass, String... ids) {
    if (ids == null || ids.length == 0) {
      return Single.just(Collections.emptyList());
    }
    return read(sql -> sql.getEntities(entityClass, ids));
  }

  /**
   * @param entityClass
   * @param ids
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<List<T>> get(Class<T> entityClass, Stream<String> ids) {
    if (ids == null) {
      return Single.just(Collections.emptyList());
    }

    return get(entityClass, ids.collect(Collectors.toList()));
  }

  /**
   * @param entityClass
   * @param ids
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<List<T>> get(Class<T> entityClass,
      Collection<String> ids) {
    if (ids == null || ids.isEmpty()) {
      return Single.just(Collections.emptyList());
    }
    return read(sql -> sql.getEntities(entityClass, ids));
  }

  /**
   * @param cls
   * @param condition
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<List<T>> select(final Class<T> cls,
      Condition condition) {
    return select(cls, condition, 1000);
  }

  /**
   * @param cls
   * @param condition
   * @param limit
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<List<T>> select(final Class<T> cls, Condition condition,
      int limit) {
    return read(sql -> sql.select(cls, condition, limit));
  }

  /**
   * @param cls
   * @param conditions
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<List<T>> select(final Class<T> cls,
      Collection<? extends Condition> conditions) {
    return select(cls, conditions, 1000);
  }

  /**
   * @param cls
   * @param conditions
   * @param limit
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<List<T>> select(final Class<T> cls,
      Collection<? extends Condition> conditions, int limit) {
    return read(sql -> sql.select(cls, conditions, limit));
  }

  /**
   * @param cls
   * @param condition
   * @param limit
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<Map<String, T>> selectMap(final Class<T> cls,
      Condition condition, int limit) {
    return read(sql -> sql.selectMap(cls, condition, limit));
  }

  /**
   * @param cls
   * @param condition
   * @param <T>
   * @return
   */
  public <T> Single<T> selectOne(final Class<T> cls, Condition condition) {
    return read(sql -> sql.selectOne(cls, condition));
  }

  /**
   * @param cls
   * @param conditions
   * @param <T>
   * @return
   */
  public <T> Single<T> selectOne(final Class<T> cls, Collection<? extends Condition> conditions) {
    return read(sql -> sql.selectOne(cls, conditions));
  }

  /**
   * @param cls
   * @param conditions
   * @param <T>
   * @return
   */
  public <T> Single<T> selectOne(final Class<T> cls, Condition... conditions) {
    return selectOne(cls, Arrays.asList(conditions));
  }

  /**
   * @param entityClass
   * @param ids
   * @param <E>
   * @return
   */
  public <E extends AbstractEntity> Single<Map<String, E>> getMap(Class<E> entityClass,
      Collection<String> ids) {
    return read(sql -> sql.getMap(entityClass, ids));
  }

  /**
   * @param entityClass
   * @param ids
   * @param <E>
   * @return
   */
  public <E extends AbstractEntity> Single<Map<String, E>> getMap(Class<E> entityClass,
      Stream<String> ids) {
    return read(sql -> sql.getMap(entityClass, ids.collect(Collectors.toList())));
  }

  /**
   * @param entityClass
   * @param toMap
   * @param ids
   * @param <E>
   * @return
   */
  public <E extends AbstractEntity> Single<Map<String, E>> getMap(Class<E> entityClass,
      Map<String, E> toMap,
      Collection<String> ids) {
    return read(sql -> sql.getMap(entityClass, toMap, ids));
  }

  /**
   * @param batch
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<int[]>> batch(
      Function<SqlBatch, SqlBatch> batch) {
    return write(sql -> batch.apply(sql.batch()).execute());
  }

  /**
   * @param batch
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<int[]>> batch(
      Function<SqlBatch, SqlBatch> batch,
      int timeoutSeconds) {
    return write(sql -> batch.apply(sql.batch()).execute(timeoutSeconds));
  }

  /**
   * @param batch
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<int[]>> batchAtomic(
      Function<SqlBatch, SqlBatch> batch) {
    return write(sql -> batch.apply(sql.batch()).executeAtomic());
  }

  /**
   * @param batch
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<int[]>> batchAtomic(
      Function<SqlBatch, SqlBatch> batch,
      int timeoutSeconds) {
    return write(sql -> batch.apply(sql.batch()).executeAtomic(timeoutSeconds));
  }

  /**
   * @param batch
   * @param logger
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<int[]>> batch(
      Function<SqlBatch, SqlBatch> batch, Logger logger) {
    return write(sql -> batch.apply(sql.batch()).execute(logger));
  }

  /**
   * @param batch
   * @param logger
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<int[]>> batch(
      Function<SqlBatch, SqlBatch> batch,
      int timeoutSeconds,
      Logger logger) {
    return write(sql -> batch.apply(sql.batch()).execute(timeoutSeconds, logger));
  }

  /**
   * @param batch
   * @param logger
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<int[]>> batchAtomic(
      Function<SqlBatch, SqlBatch> batch, Logger logger) {
    return write(sql -> batch.apply(sql.batch()).executeAtomic(logger));
  }

  /**
   * @param batch
   * @param logger
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<int[]>> batchAtomic(
      Function<SqlBatch, SqlBatch> batch,
      int timeoutSeconds,
      Logger logger) {
    return write(sql -> batch.apply(sql.batch()).executeAtomic(timeoutSeconds, logger));
  }

  /**
   * @param entity
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<Integer>> insert(T entity) {
    return write(sql -> sql.insert(entity));
  }

  /**
   * @param entity
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<Integer>> insertAtomic(T entity) {
    return write(sql -> sql.insertAtomic(entity));
  }

  /**
   * @param entities
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<int[]>> insert(List<T> entities) {
    return write(sql -> sql.insert(entities));
  }

  /**
   * @param entities
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<int[]>> insertAtomic(
      List<T> entities) {
    return write(sql -> sql.insertAtomic(entities));
  }

  /**
   * @param entities
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<int[]>> insertAtomic(List<T> entities,
      int timeoutSeconds) {
    return write(sql -> sql.insertAtomic(entities, timeoutSeconds));
  }

  /**
   * @param entity
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<Integer>> update(T entity) {
    return write(sql -> sql.update(entity));
  }

  /**
   * @param entity
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<Integer>> updateAtomic(T entity) {
    return write(sql -> sql.updateAtomic(entity));
  }

  /**
   * @param entities
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<int[]>> update(List<T> entities) {
    return write(sql -> sql.update(entities));
  }

  /**
   * @param entities
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<int[]>> update(List<T> entities,
      int timeoutSeconds) {
    return write(sql -> sql.update(entities, timeoutSeconds));
  }

  /**
   * @param entities
   * @param <T>
   * @return
   */
  public <T extends AbstractEntity> Single<move.sql.SqlResult<int[]>> updateAtomic(
      List<T> entities) {
    return write(sql -> sql.updateAtomic(entities));
  }

  public <T> Single<move.sql.SqlResult<T>> write(SqlCallable<T> task) {
    return rxWrite(task);
  }

  public <T> Single<move.sql.SqlResult<T>> write(SqlCallable<T> task, int timeoutMillis) {
    return rxWrite(task, timeoutMillis);
  }

  public <T> Single<move.sql.SqlResult<T>> rxWrite(SqlCallable<T> task) {
    return rxWrite(task, writeTaskTimout);
  }

  public <T> Single<move.sql.SqlResult<T>> rxWrite(SqlCallable<T> task, int timeoutMillis) {
    final MoveEventLoop eventLoop = MoveKernel.INSTANCE.getOrCreateContext();

    final SqlAction action = new SqlAction(eventLoop, timeoutMillis);

    return writeExecutor
        .rxExecuteBlocking(new Handler<io.vertx.rxjava.core.Future<SqlResult<T>>>() {
          @Override
          public void handle(io.vertx.rxjava.core.Future<SqlResult<T>> event) {
            try {
              final SqlResult<T> value = executeWrite(action, task);
              if (eventLoop != null) {
                eventLoop.execute(() -> event.complete(value));
              } else {
                vertx.getOrCreateContext().runOnContext(a -> event.complete(value));
              }
            } catch (Exception e) {
              try {
                final Throwable cause = Throwables.getRootCause(e);
                final Statement statement = action.currentStatement;
                if (statement != null) {
                  rogueStatementQueue
                      .add(new Tuple2<>(statement, Boolean.FALSE));
                }
                if (cause instanceof ActionTimeoutException
                    || cause instanceof InterruptedException) {
                  writeTimeoutsCounter.inc();
                } else {
                  writeExceptionsCounter.inc();
                }
              } finally {
                if (eventLoop != null) {
                  eventLoop.execute(() -> event.fail(e));
                } else {
                  vertx.getOrCreateContext().runOnContext(a -> event.fail(e));
                }
              }
            }
          }
        });
  }

  /**
   * @param task
   * @param handler
   * @param <T>
   */
  public <T> void write(SqlCallable<T> task, Handler<AsyncResult<move.sql.SqlResult<T>>> handler) {
    final io.vertx.rxjava.core.Context context = vertx.getOrCreateContext();

    rxWrite(task).subscribe(
        r -> {
          context.runOnContext(a -> handler.handle(Future.succeededFuture(r)));
        },
        e -> {
          context.runOnContext(a -> handler.handle(Future.failedFuture(e)));
        }
    );
  }

  public <T> Single<T> read(SqlReadCallable<T> task) {
    return rxSingle(task);
  }

  public <T> Single<T> read(SqlReadCallable<T> task, int timeoutMillis) {
    return rxSingle(task, timeoutMillis);
  }

  public <T> Single<T> rxSingle(SqlReadCallable<T> task) {
    return rxSingle(task, readTaskTimout);
  }

  public <T> Single<T> rxSingle(SqlReadCallable<T> task, int timeoutMillis) {
    final MoveEventLoop eventLoop = MoveKernel.INSTANCE.getOrCreateContext();

    final SqlAction action = new SqlAction(eventLoop, timeoutMillis);

    return readExecutor.rxExecuteBlocking(new Handler<io.vertx.rxjava.core.Future<T>>() {
      @Override
      public void handle(io.vertx.rxjava.core.Future<T> event) {
        try {
          final T value = executeRead(action, task);
          if (eventLoop != null) {
            eventLoop.execute(() -> event.complete(value));
          } else {
            vertx.getOrCreateContext().runOnContext(a -> event.complete(value));
          }
        } catch (Exception e) {
          try {
            final Throwable cause = Throwables.getRootCause(e);
            final Statement statement = action.currentStatement;
            if (statement != null) {
              rogueStatementQueue
                  .add(new Tuple2<>(statement, Boolean.FALSE));
            }
            if (cause instanceof ActionTimeoutException
                || cause instanceof InterruptedException) {
              readTimeoutsCounter.inc();
            } else {
              readExceptionsCounter.inc();
            }
          } finally {
            if (eventLoop != null) {
              eventLoop.execute(() -> event.fail(e));
            } else {
              vertx.getOrCreateContext().runOnContext(a -> event.fail(e));
            }
          }
        }
      }
    });
  }

  /**
   * @param task
   * @param handler
   * @param <T>
   */
  public <T> void read(final SqlReadCallable<T> task, final Handler<AsyncResult<T>> handler) {
    final io.vertx.rxjava.core.Context context = vertx.getOrCreateContext();

    rxSingle(task).subscribe(
        r -> {
          context.runOnContext(a -> handler.handle(Future.succeededFuture(r)));
        },
        e -> {
          context.runOnContext(a -> handler.handle(Future.failedFuture(e)));
        }
    );
  }

  public <T> T readBlocking(SqlReadCallable<T> task) {
    return rxSingle(task).toBlocking().value();
  }

  /**
   * @param task
   * @param <T>
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public <T> T readBlocking(SqlReadCallable<T> task, int timeoutMillis) {
    final SqlAction action = new SqlAction(null, timeoutMillis);

    return readExecutor.rxExecuteBlocking(new Handler<io.vertx.rxjava.core.Future<T>>() {
      @Override
      public void handle(io.vertx.rxjava.core.Future<T> event) {
        try {
          final T value = executeRead(action, task);
          event.complete(value);
        } catch (Exception e) {
          try {
            final Throwable cause = Throwables.getRootCause(e);
            final Statement statement = action.currentStatement;
            if (statement != null) {
              rogueStatementQueue
                  .add(new Tuple2<>(statement, Boolean.FALSE));
            }
            if (cause instanceof ActionTimeoutException
                || cause instanceof InterruptedException) {
              readTimeoutsCounter.inc();
            } else {
              readExceptionsCounter.inc();
            }
          } finally {
            event.fail(e);
          }
        }
      }
    }).toBlocking().value();
  }

  public <T> move.sql.SqlResult<T> writeBlocking(SqlCallable<T> task) {
    return writeBlocking(task, writeTaskTimout);
  }

  /**
   * @param task
   * @param <T>
   * @throws ExecutionException
   * @throws InterruptedException
   */
  public <T> move.sql.SqlResult<T> writeBlocking(SqlCallable<T> task, int timeoutMillis) {
    final SqlAction action = new SqlAction(null, timeoutMillis);

    return writeExecutor
        .rxExecuteBlocking(new Handler<io.vertx.rxjava.core.Future<SqlResult<T>>>() {
          @Override
          public void handle(io.vertx.rxjava.core.Future<SqlResult<T>> event) {
            try {
              final SqlResult<T> value = executeWrite(action, task);
              event.complete(value);
            } catch (Exception e) {
              try {
                final Throwable cause = Throwables.getRootCause(e);
                final Statement statement = action.currentStatement;
                if (statement != null) {
                  rogueStatementQueue
                      .add(new Tuple2<>(statement, Boolean.FALSE));
                }
                if (cause instanceof ActionTimeoutException
                    || cause instanceof InterruptedException) {
                  writeTimeoutsCounter.inc();
                } else {
                  writeExceptionsCounter.inc();
                }
              } finally {
                event.fail(e);
              }
            }
          }
        }).toBlocking().value();
  }

  private Timer getReadTimerFor(String name) {
    if (name == null || name.isEmpty()) {
      return null;
    }

    Timer timer = readTimerMap.get(name);

    if (timer != null) {
      return timer;
    }

    synchronized (this) {
      timer = readTimerMap.get(name);

      if (timer != null) {
        return timer;
      }

      timer = metricRegistry
          .timer(
              "SQL-" + config.getName() + "-READ-" + name
          );

      readTimerMap.put(name, timer);
    }

    return timer;
  }

  private Timer getWriteTimerFor(String name) {
    if (name == null || name.isEmpty()) {
      return null;
    }

    Timer timer = writeTimerMap.get(name);

    if (timer != null) {
      return timer;
    }

    synchronized (this) {
      timer = writeTimerMap.get(name);

      if (timer != null) {
        return timer;
      }

      timer = metricRegistry
          .timer(
              "SQL-" + config.getName() + "-WRITE-" + name
          );

      writeTimerMap.put(name, timer);
    }

    return timer;
  }

  /**
   * @param callable
   * @param <T>
   * @return
   */
  protected <T> T executeRead(SqlAction action, SqlReadCallable<T> task) {
    final Connection connection;

    final Timer contextTimer = getReadTimerFor(action.callingActionName);
    final Timer.Context contextTimerContext = contextTimer != null
        ? contextTimer.time()
        : null;
    final Timer.Context timerContext = readTimer.time();
    try {
      connection = readDataSource.getConnection();
    } catch (Throwable e) {
      if (contextTimerContext != null) {
        contextTimerContext.stop();
      }
      timerContext.stop();
      throw new PersistException(e);
    }

    try {
      final ConnectionProvider connectionProvider = new DefaultConnectionProvider(connection);
      final Configuration configuration = readConfiguration.derive(connectionProvider);

      if (action != null) {
        configuration.data(ACTION_KEY, action);
      }

      try {
        final T result = DSL.using(configuration).connectionResult(new ConnectionCallable<T>() {

          public T run(Connection connection) throws Exception {
            return task
                .call(new SqlSession(SqlDatabase.this, configuration, connection));
          }
        });

        return result;
      } finally {
        if (action != null) {
          configuration.data().remove(ACTION_KEY);
        }
      }
    } catch (Throwable e) {
      timerContext.stop();
      throw new PersistException(e);
    } finally {
      Try.run(() -> connection.close()).onFailure(e -> LOG.error("Failed to close connection", e));

      if (contextTimerContext != null) {
        contextTimerContext.stop();
      }
      timerContext.stop();
    }
  }

  /**
   * @param callable
   * @param <T>
   * @return
   */
  protected <T> move.sql.SqlResult<T> executeWrite(SqlAction action, SqlCallable<T> task) {
    final AtomicReference<move.sql.SqlResult<T>> r = new AtomicReference<>();
    final Connection connection;

    final Timer contextTimer = getWriteTimerFor(action.callingActionName);
    final Timer.Context contextTimerContext = contextTimer != null
        ? contextTimer.time()
        : null;
    final Timer.Context timerContext = writeTimer.time();
    try {
      connection = dataSource.getConnection();
    } catch (Throwable e) {
      if (contextTimerContext != null) {
        contextTimerContext.stop();
      }
      timerContext.stop();
      throw new PersistException(e);
    }

    try {
      final DefaultConnectionProvider connectionProvider = new DefaultConnectionProvider(
          connection);
      final Configuration configuration = this.configuration.derive(connectionProvider);
      configuration.set(new ThreadLocalTransactionProvider(connectionProvider));

      if (action != null) {
        configuration.data(ACTION_KEY, action);
      }

      try {
        final SqlSession session = new SqlSession(this, configuration, connection);

        return DSL.using(session.configuration()).transactionResult($ -> {
          session.scope($);

          try {
            // Execute the code.
            move.sql.SqlResult<T> result = null;

            try {
              result = task.call(session);

              if (result == null) {
                result = move.sql.SqlResult.commit();
              }
            } catch (Throwable e) {
              result = move.sql.SqlResult.rollback(null, e);
            }

            r.set(result);

            // Rollback if ActionResponse isFailure.
            if (!result.isSuccess() || result.getReason() != null) {
              throw new RollbackException();
            }

            return result;
          } finally {
            session.unscope();
          }
        });
      } finally {
        if (action != null && configuration.data() != null) {
          configuration.data().remove(ACTION_KEY);
        }
      }
    } catch (RollbackException e) {
      if (r.get().getReason() != null) {
        final Throwable cause = r.get().getReason();
        Throwables.throwIfUnchecked(cause);
        throw new RuntimeException(cause);
      } else {
        return r.get();
      }
    } catch (Throwable e) {
      LOG.info("write() threw an exception", e);
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    } finally {
      Try.run(() -> connection.close()).onFailure(e -> LOG.error("Failed to close connection", e));
      if (contextTimerContext != null) {
        contextTimerContext.stop();
      }
      timerContext.stop();
    }
  }

  /**
   * @param cls
   * @return
   */
  TableMapping findMapping(Class cls) {
    return mappings.get(cls);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////
  // Mappings
  ////////////////////////////////////////////////////////////////////////////////////////////////////

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
      throw new PersistException(
          "Mapping for class [" + cls.getCanonicalName() + "] was not found.");
    }
    return mapping;
  }

  /**
   * @param mapping
   * @param <R>
   * @param <E>
   * @return
   */
  <R extends Record, E extends AbstractEntity> RecordMapper<R, E> recordMapper(
      TableMapping mapping) {
    return (RecordMapper<R, E>) mapping.getRecordMapper();
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

////////////////////////////////////////////////////////////////////////////////////////////////////
// jOOQ Impl
////////////////////////////////////////////////////////////////////////////////////////////////////

  public Configuration getReadConfiguration() {
    return readConfiguration;
  }

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


    protected void startUp() throws Exception {
      tcpServer = new TcpServer();
      tcpServer.init("-tcpPort", Integer.toString(port));
      server = new Server(tcpServer);
      server.start();
    }


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

    public void executeStart(ExecuteContext ctx) {
      if (!LOG.isDebugEnabled()) {
        return;
      }

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

  public static class ReadRequest<T> {

    public final SqlDatabase database;
    public final SqlReadCallable<T> lambda;

    public ReadRequest(SqlDatabase database, SqlReadCallable<T> lambda) {
      this.database = database;
      this.lambda = lambda;
    }
  }

  public static class WriteRequest<T> {

    public final SqlDatabase database;
    public final SqlCallable<T> lambda;

    public WriteRequest(SqlDatabase database, SqlCallable<T> lambda) {
      this.database = database;
      this.lambda = lambda;
    }
  }

  public static final class SqlReply<T> {

    public final T value;

    public SqlReply(T value) {
      this.value = value;
    }
  }

  public static class SqlAction {

    MoveEventLoop eventLoop;
    String callingActionName = "";
    Statement currentStatement;
    JobAction job;
    long timeoutMillis;
    long deadline;

    public SqlAction(MoveEventLoop eventLoop, long timeoutMillis) {
      this.eventLoop = eventLoop;

      if (eventLoop != null) {
        this.job = eventLoop.getJob();
        if (job != null) {
          callingActionName = job.getProvider().getActionClass().getCanonicalName();

          if (job.getDeadline() > 0L) {
            timeoutMillis = Math.min(
                (job.getDeadline() * MoveEventLoop.TICK_MS) - System.currentTimeMillis(),
                timeoutMillis
            );
          }
        }
      }

      this.timeoutMillis = timeoutMillis;
      this.deadline = System.currentTimeMillis() + timeoutMillis;
    }
  }

//    public class DeleteOrUpdateWithoutWhereListener extends DefaultExecuteListener {
//
//        
//        public void renderEnd(ExecuteContext eventLoop) {
//            try {
//                if (eventLoop.batchQueries() != null) {
//                    for (Query query : eventLoop.batchQueries()) {
//                        if (query.getSQL().matches("^(?i:(UPDATE|DELETE)(?!.* WHERE ).*)$")) {
//                            throw new DeleteOrUpdateWithoutWhereException();
//                        }
//                    }
//                }
//            } catch (Throwable e) {
//                LOG.error("DeleteOrUpdateWithoutWhereListener Error", e);
//                Throwables.propagate(e);
//            }
//        }
//    }
//
//    public class DeleteOrUpdateWithoutWhereException extends RuntimeException {
//    }

  /**
   * Sets the JDBC Statement deadline based on current ActionContext.
   */
  public class TimeoutListener extends DefaultExecuteListener {


    public void executeStart(ExecuteContext ctx) {
      super.executeStart(ctx);
      try {
        if (ctx.statement() != null) {
          int queryTimeout = ctx.statement().getQueryTimeout();
          final Object actionObj = ctx.configuration().data(ACTION_KEY);

          if (actionObj != null && actionObj instanceof JobAction) {
            final SqlAction action = (SqlAction) actionObj;
            action.currentStatement = ctx.statement();

            if (action.deadline > 0) {
              int timeLeft = (int) (action.deadline - System.currentTimeMillis());

              if (timeLeft <= 1000) {
                queryTimeout = 1;
              } else {
                queryTimeout = (int) Math.ceil((double) timeLeft / 1000.0);
              }

              ctx.statement().setQueryTimeout(queryTimeout);
            } else if (queryTimeout < 30) {
              ctx.statement().setQueryTimeout(30);
            }
          } else {
            if (queryTimeout < 30) {
              ctx.statement().setQueryTimeout(30);
            }
          }
        }
      } catch (Throwable e) {
        // Ignore.
      }
    }


    public void executeEnd(ExecuteContext ctx) {
      super.executeEnd(ctx);

      try {
        if (ctx.statement() != null) {
          int queryTimeout = ctx.statement().getQueryTimeout();
          final Object actionObj = ctx.configuration().data(ACTION_KEY);

          if (actionObj != null && actionObj instanceof SqlAction) {
            final SqlAction action = (SqlAction) actionObj;
            action.currentStatement = null;
          }
        }
      } catch (Throwable e) {
        // Ignore.
      }
    }
  }

  /**
   *
   */
  private final class RecordMapperProviderImpl implements RecordMapperProvider {

    private final DefaultRecordMapperProvider defaultProvider;

    public RecordMapperProviderImpl() {
      defaultProvider = new DefaultRecordMapperProvider();
    }


    public <R extends Record, E> RecordMapper<R, E> provide(RecordType<R> recordType,
        Class<? extends E> type) {
      final TableMapping mapping = tableMappings.get(type);
      if (mapping == null) {
        return defaultProvider.provide(recordType, type);
      }
      return (RecordMapper<R, E>) mapping.getRecordMapper();
    }
  }

  /**
   * Cleans rogue NuoDB JDBC Statements. Calls the SQL "KILL STATEMENT connid ? handle ? count -1"
   * as used by
   */
  private final class NuoDBStatementCleaner extends AbstractExecutionThreadService {

    private final DataSource dataSource;
    private final DataSource readDataSource;
    private final java.lang.reflect.Field nuoHandleField;

    public NuoDBStatementCleaner(DataSource dataSource, DataSource readDataSource) {
      this.dataSource = dataSource;
      this.readDataSource = readDataSource;
      try {
        this.nuoHandleField = RemStatement.class.getDeclaredField("handle");
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
    }

    protected void run() throws Exception {
      while (isRunning()) {
        final Tuple2<Statement, Boolean> request =
            rogueStatementQueue.poll(2, TimeUnit.SECONDS);

        if (request == null) {
          continue;
        }

        Try.run(
            () -> rogueStatementsCounter.inc()
        ).onFailure(
            e -> LOG.error("rogueStatementsCounter.inc() threw an exception", e)
        );

        if (request._1 == null) {
          LOG.error("NuoDBStatementCleaner received a null statement");
          continue;
        }

        if (!(request._1 instanceof RemStatement)) {
          LOG.error(
              "NuoDBStatementCleaner only handles com.nuodb.jdbc.RemStatement. Found " + request
                  .getClass().getCanonicalName());
          Try.run(() -> request._1.cancel());
          continue;
        }

        final RemStatement remStatement = (RemStatement) request._1;
        RemConnection remConnection = (RemConnection) remStatement.getConnection();
        remConnection.getServerSideConnectionId();
        final Object handleObj = nuoHandleField.get(remStatement);

        Connection connection = null;
        if (request._2 == Boolean.TRUE) {
          try {
            connection = readDataSource.getConnection();
          } catch (Throwable e) {
            LOG.error("NuoDBStatementCleaner.readDataSource.getConnection() threw an exception", e);
          }
          Try.run(
              () -> rogueReadStatementsCounter.inc()
          ).onFailure(e -> LOG.error("rogueReadStatementsCounter.inc() threw an exception", e));
        } else {
          try {
            connection = dataSource.getConnection();
          } catch (Throwable e) {
            LOG.error("NuoDBStatementCleaner.dataSource.getConnection() threw an exception", e);
          }
          Try.run(
              () -> rogueWriteStatementsCounter.inc()
          ).onFailure(e -> LOG.error("rogueWriteStatementsCounter.inc() threw an exception", e));
        }

        if (connection == null) {
          rogueStatementQueue.add(request);
          continue;
        }

        try (final Connection killCon = connection) {
          try (PreparedStatement pstmt = killCon
              .prepareStatement("KILL STATEMENT connid ? handle ? count -1")) {
            pstmt.setInt(1, remConnection.getServerSideConnectionId());
            pstmt.setInt(2, (int) handleObj);
            pstmt.execute();
          }
        } catch (Throwable e) {
          final Throwable root = Throwables.getRootCause(e);
          if (root instanceof SQLException) {
            final SQLException ex = (SQLException) root;
            if (Strings.nullToEmpty(ex.getMessage()).toLowerCase()
                .contains("already been killed")) {
              return;
            }
          }
          LOG.error("StatementCleaner service caught an exception while calling KILL STATEMENT ",
              e);
          Try.run(
              () -> rogueStatementExceptionsCounter.inc()
          ).onFailure(
              e2 -> LOG.error("rogueWriteStatementsCounter.inc() threw an exception", e2));
        }
      }
    }
  }

  /**
   * Cleans rogue JDBC Statements. Uses the Statement.cancel() API to cancel a statement.
   */
  private final class StatementCleaner extends AbstractExecutionThreadService {

    private java.lang.reflect.Field nuoHandleField;


    protected void run() throws Exception {
      while (isRunning()) {
        final Tuple2<Statement, Boolean> request = rogueStatementQueue.poll(2, TimeUnit.SECONDS);

        if (request == null) {
          continue;
        }

        Try.run(
            () -> rogueStatementsCounter.inc()
        ).onFailure(
            e -> LOG.error("rogueStatementsCounter.inc() threw an exception", e)
        );

        if (request._2 == Boolean.TRUE) {
          Try.run(
              () -> rogueReadStatementsCounter.inc()
          ).onFailure(e -> LOG.error("rogueReadStatementsCounter.inc() threw an exception", e));
        } else {
          Try.run(
              () -> rogueWriteStatementsCounter.inc()
          ).onFailure(e -> LOG.error("rogueWriteStatementsCounter.inc() threw an exception", e));
        }

        try {
          request._1.cancel();
        } catch (Throwable e) {
          LOG.warn("StatementCleaner service caught an exception while calling _request.cancel()",
              e);
          Try.run(
              () -> rogueStatementExceptionsCounter.inc()
          ).onFailure(e2 -> LOG.error("rogueWriteStatementsCounter.inc() threw an exception", e2));
        }
      }
    }
  }
}
