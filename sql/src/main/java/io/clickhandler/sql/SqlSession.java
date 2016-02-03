package io.clickhandler.sql;

import com.google.common.base.Preconditions;
import com.google.common.collect.*;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultRecordListenerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Manages the logical lifecycle of a Transaction.
 */
@SuppressWarnings("unchecked")
public class SqlSession {
    private static final Logger LOG = LoggerFactory.getLogger(SqlSession.class);
    private static final SqlResult success = SqlResult.success();
    private static final SqlResult rollback = SqlResult.rollback();
    protected final SqlDatabase db;
    protected final Stack<Configuration> configurationStack = new Stack<>();
    private final RecordListener recordListener = new RecordListenerImpl();
    private Multimap<TableMapping, AbstractEntity> cachePuts;
    private Multimap<TableMapping, String> cacheInvalidates;
    private Configuration configuration;

    SqlSession(final SqlDatabase db,
               final Configuration configuration) {
        this.db = db;
        this.configuration = configuration.derive(new DefaultRecordListenerProvider(recordListener));

    }

    protected Multimap<TableMapping, AbstractEntity> getCachePuts() {
        if (cachePuts == null) {
            cachePuts = Multimaps.newSetMultimap(Maps.newHashMap(), Sets::newHashSet);
        }
        return cachePuts;
    }

    protected Multimap<TableMapping, String> getCacheInvalidates() {
        if (cacheInvalidates == null) {
            cacheInvalidates = Multimaps.newSetMultimap(Maps.newHashMap(), Sets::newHashSet);
        }
        return cacheInvalidates;
    }

    public SqlResult success() {
        return success;
    }

    public <T> SqlResult<T> success(T result) {
        return SqlResult.success(result);
    }

    public SqlResult rollback() {
        return rollback;
    }

    public <T> SqlResult<T> rollback(T result) {
        return SqlResult.rollback(result);
    }

    /**
     * @return
     */
    public DSLContext create() {
        return DSL.using(configuration);
    }

    /**
     * @return
     */
    public Configuration configuration() {
        return configuration;
    }

    /**
     * @param configuration
     */
    void scope(Configuration configuration) {
        configurationStack.push(this.configuration);
        this.configuration = configuration;
    }

    /**
     *
     */
    void unscope() {
        this.configuration = configurationStack.pop();
    }

    /**
     * @return
     */
    public Connection connection() {
        final Connection connection = configuration.connectionProvider().acquire();

        if (connection == null) {
            throw new PersistException("Session is no longer active.");
        }

        return connection;
    }

    /**
     * @param recordClass
     * @param <R>
     * @return
     */
    public <R extends UpdatableRecord<R>> R record(Class<R> recordClass) {
        if (recordClass == null) {
            return null;
        }
        final TableMapping mapping = db.getMapping(recordClass);
        if (mapping == null) {
            return null;
        }
        final R record = (R) mapping.TBL().newRecord();
        record.attach(configuration);
        return record;
    }

    /**
     * @param record
     * @param <R>
     * @return
     */
    public <R extends UpdatableRecord<R>> R attach(R record) {
        if (record == null) {
            return null;
        }
        record.attach(configuration);
        return record;
    }

    /**
     * @param records
     * @param <R>
     * @return
     */
    public <R extends UpdatableRecord<R>> void attach(Collection<R> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (R record : records) {
            record.attach(configuration);
        }
    }

    /**
     * @param records
     * @param <R>
     * @return
     */
    public <R extends UpdatableRecord<R>> void attach(List<R> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (int i = 0; i < records.size(); i++) {
            records.get(i).attach(configuration);
        }
    }

    public <E extends AbstractEntity, R extends UpdatableRecord<R>> R map(E entity) {
        Preconditions.checkNotNull(entity);
        final EntityMapper<E, R> mapper = db.entityMapper(entity.getClass());
        Preconditions.checkNotNull(mapper);
        return mapper.map(entity);
    }

    public <E extends AbstractEntity, R extends UpdatableRecord<R>> EntityMapper<E, R> mapper(Class cls) {
        Preconditions.checkNotNull(cls);
        return db.entityMapper(cls);
    }

    public <E extends AbstractEntity, R extends UpdatableRecord<R>> List<R> mapRecords(Collection<E> entities) {
        if (entities == null || entities.isEmpty()) {
            return Lists.newArrayListWithCapacity(0);
        }

        EntityMapper<E, R> mapper = null;
        for (E entity : entities) {
            mapper = db.entityMapper(entity.getClass());
            break;
        }

        return mapRecords(mapper, entities);
    }

    /**
     * @param entities
     * @param <R>
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity, R extends UpdatableRecord<R>> List<R> mapRecords(EntityMapper<E, R> mapper, Collection<E> entities) {
        if (entities == null || entities.isEmpty()) {
            return Lists.newArrayListWithCapacity(0);
        }
        Preconditions.checkNotNull(mapper, "mapper was null");

        final List<R> records = Lists.newArrayListWithCapacity(entities.size());
        for (E entity : entities) {
            records.add(mapper.map(entity));
        }

        // Attach all of the records.
        attach(records);

        return records;
    }

    /**
     * @param recordClass
     * @param id
     * @param <R>
     * @return
     */
    public <R extends UpdatableRecord<R>> R getRecord(Class<R> recordClass, String id) {
        return getRecord(db.getMapping(recordClass), id);
    }

    public <R extends UpdatableRecord<R>> R newRecord(Class<R> recordClass) {
        final TableMapping mapping = db.getCheckedMapping(recordClass);
        final R record = (R) mapping.tbl.newRecord();
        record.attach(configuration);
        return record;
    }

    /**
     * @param recordClass
     * @param id
     * @param <R>
     * @return
     */
    public <R extends UpdatableRecord<R>> R getRecordOrNew(Class<R> recordClass, String id) {
        final TableMapping mapping = db.getMapping(recordClass);
        R record = getRecord(mapping, id);
        if (record != null) {
            return record;
        }

        record = (R) mapping.tbl.newRecord();
        record.attach(configuration);
        return record;
    }

    /**
     * @param mapping
     * @param id
     * @param <R>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <R extends UpdatableRecord<R>> R getRecord(TableMapping mapping, String id) {
        return (R) create().selectFrom(mapping.TBL()).where(mapping.ID().eq(id)).fetchOne();
    }

    /**
     * @param recordClass
     * @param id
     * @param <R>
     * @return
     */
    public <R extends UpdatableRecord<R>> List<R> getRecords(Class<R> recordClass, String... id) {
        return getRecords(recordClass, Lists.newArrayList(id));
    }

    /**
     * @param recordClass
     * @param ids
     * @param <R>
     * @return
     */
    public <R extends UpdatableRecord<R>> List<R> getRecords(Class<R> recordClass, Collection<String> ids) {
        return getRecords(db.getMapping(recordClass), ids);
    }

    /**
     * @param mapping
     * @param ids
     * @param <R>
     * @return
     */
    public <R extends UpdatableRecord<R>> List<R> getRecords(TableMapping mapping, Collection<String> ids) {
        return getRecords(mapping, ids, 255);
    }

    /**
     * @param mapping
     * @param collection
     * @param <R>
     * @return
     */
    public <R extends UpdatableRecord<R>> List<R> getRecords(final TableMapping mapping,
                                                             final Collection<String> collection,
                                                             final int batchSize) {
        Preconditions.checkNotNull(mapping);

        if (collection == null) {
            return null;
        }
        if (collection.isEmpty()) {
            return new ArrayList<>();
        }

        // Optimize by selecting IDs in sort order.
        final List<String> ids;
        final Collection<String> originalSort;
        if (collection instanceof List) {
            ids = (List<String>) collection;
            originalSort = Lists.newArrayList(ids);
        } else {
            ids = Lists.newArrayList(collection);
            originalSort = collection;
        }

        if (ids.size() <= batchSize) {
            return fetch(mapping, ids);
        }

        // Copy and store original sort order.
        Collections.sort(ids);

        final List<R> records = Lists.newArrayListWithCapacity(ids.size());
        final List<String> fetchList = new ArrayList<>(ids.size() > batchSize ? batchSize : ids.size());

        for (String key : ids) {
            fetchList.add(key);

            if (fetchList.size() == batchSize) {
                fetch(mapping, records, fetchList);
                fetchList.clear();
            }
        }

        if (!fetchList.isEmpty()) {
            fetch(mapping, records, fetchList);
            fetchList.clear();
        }

        final List<R> sorted = new ArrayList<>(records.size());
        for (String key : originalSort) {
            for (R e : records) {
                if (key.equals(e.getValue(mapping.ID()))) {
                    sorted.add(e);
                    break;
                }
            }
        }

        return sorted;
    }

    /**
     * @param cls
     * @param map
     * @param <R>
     */
    public <R extends UpdatableRecord> void getRecords(Class cls, Map<String, R> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        final TableMapping mapping = db.getCheckedMapping(cls);
        final List<R> records = getRecords(cls, map.keySet());

        if (records != null) {
            for (R record : records) {
                if (record == null) {
                    continue;
                }

                map.put(record.getValue(mapping.ID()), record);
            }
        }
    }

    /**
     * @param mapping
     * @param recordList
     * @param ids
     * @param <R>
     */
    protected <R extends UpdatableRecord<R>> void fetch(TableMapping mapping, List<R> recordList, Collection<String> ids) {
        final List<R> result = (List<R>) create().selectFrom(mapping.TBL()).where(mapping.ID().in(ids)).fetch();
        if (result != null && !result.isEmpty()) {
            recordList.addAll(result);
        }
    }

    /**
     * @param mapping
     * @param ids
     * @param <R>
     * @return
     */
    protected <R extends UpdatableRecord<R>> List<R> fetch(TableMapping mapping, Collection<String> ids) {
        return (List<R>) create().selectFrom(mapping.TBL()).where(mapping.ID().in(ids)).fetch();
    }

    /**
     * @param cls
     * @param id
     * @param <E>
     * @return
     */
    public <R extends UpdatableRecord<R>, E extends AbstractEntity> E getEntity(Class<E> cls, String id) {
        return db.map((R) getRecord(db.getCheckedMapping(cls), id));
    }

    /**
     * @param entityClass
     * @param ids
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends UpdatableRecord<R>> List<E> getEntities(Class entityClass, String... ids) {
        return getEntities(entityClass, Lists.newArrayList(ids));
    }

    /**
     * @param entityClass
     * @param ids
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends UpdatableRecord<R>> List<E> getEntities(Class entityClass, Collection<String> ids) {
        return db.map((List<R>) getRecords(db.getMapping(entityClass), ids));
    }

    /**
     * @param entityClass
     * @param map
     * @param <E>
     */
    public <E extends AbstractEntity> void getEntities(Class entityClass, Map<String, E> map) {
        if (map == null || map.isEmpty()) {
            return;
        }

        final List<E> entities = getEntities(entityClass, map.keySet());

        if (entities != null && !entities.isEmpty()) {
            for (E entity : entities) {
                if (entity == null) {
                    continue;
                }

                map.put(entity.getId(), entity);
            }
        }
    }

    public SelectWhereStep<?> selectFrom(Class cls) {
        final TableMapping mapping = db.getCheckedMapping(cls);
        return create().selectFrom(mapping.TBL());
    }

    /**
     * @param objectClass
     * @param id
     * @param userId
     * @return
     */
    public boolean isVisible(Class objectClass,
                             String id,
                             String userId) {
        return false;
    }

    /**
     * Executes an update and returns the number of rows modified.
     * <p/>
     * Parameters will be set according with the correspondence defined in
     *
     * @param sql        SQL code to be executed.
     * @param parameters Parameters to be used in the PreparedStatement.
     * @since 1.0
     */
    public int sqlUpdate(final String sql, final Object... parameters) {
        return sqlUpdate(sql, parameters, statement -> {
            try {
                SqlUtils.setParameters(statement, parameters);
                return statement.executeUpdate();
            } catch (SQLException e) {
                throw new PersistException("Error executing sql [" + sql + "] with parameters "
                    + Arrays.toString(parameters) + ": " + e.getMessage(), e);
            }
        });
    }

    /**
     * Creates a {@link java.sql.PreparedStatement} with no parameters.
     *
     * @param sql SQL statement to create the {@link java.sql.PreparedStatement} from
     * @since 1.0
     */
    public <T> T sqlUpdate(final String sql, Object[] parameters, StatementCallback<T> callback) {
        return sqlUpdate(sql, parameters, null, callback);
    }

    /**
     * Executes an update and return a object containing the
     * number of rows modified and auto-generated keys produced.
     * <p/>
     * Parameters will be set according with the correspondence defined in
     *
     * @param sql               SQL code to be executed.
     * @param autoGeneratedKeys List of columns that are going to be
     *                          auto-generated during the query execution.
     * @param parameters        Parameters to be used in the PreparedStatement.
     * @since 1.0
     */
    public int sqlUpdate(final Object obj,
                         final String sql,
                         final String[] autoGeneratedKeys,
                         final Object... parameters) {
        return sqlUpdate(sql, parameters, autoGeneratedKeys, statement -> {
            int rowsModified;
            try {
                rowsModified = statement.executeUpdate();
            } catch (SQLException e) {
                throw new PersistException("Error executing sql [" + sql + "] with parameters "
                    + Arrays.toString(parameters) + ": " + e.getMessage(), e);
            }

            if (autoGeneratedKeys.length != 0) {
                try {
                    final Mapping mapping = db.findMapping(obj.getClass());
                    final ResultSet resultSet = statement.getGeneratedKeys();

                    try {
                        for (String autoGeneratedKey : autoGeneratedKeys) {
                            resultSet.next();

                            // get the auto-generated key using the ResultSet.get method
                            // that matches
                            // the bean setter parameter type
                            final Mapping.Property property = mapping.getProperty(autoGeneratedKey);

                            if (property == null) {
                                throw new PersistException("Column [" + autoGeneratedKey
                                    + "] from result set does not have a mapping to a field in ["
                                    + obj.getClass().getName() + "]");
                            }

                            final Class type = property.getType();
                            final Object value = SqlUtils.getValue(resultSet, 1, type);

                            try {
                                property.set(obj, value);
                            } catch (Exception e) {
                                throw new PersistException(e);
                            }
                        }
                    } finally {
                        resultSet.close();
                    }
                } catch (SQLException e) {
                    throw new PersistException("This JDBC driver does not support PreparedStatement.getGeneratedKeys()."
                        + " Please use setUpdateAutoGeneratedKeys(false) in your SqlSession instance"
                        + " to disable attempts to use that feature.");
                }
            }

            return rowsModified;
        });
    }

    /**
     * Creates a {@link java.sql.PreparedStatement}, setting the names of the
     * auto-generated keys to be retrieved.
     *
     * @param sql               SQL statement to create the {@link java.sql.PreparedStatement} from
     * @param autoGeneratedKeys names of the columns that will have
     *                          auto-generated values produced during the execution of the
     *                          {@link java.sql.PreparedStatement}
     * @since 1.0
     */
    public <T> T sqlUpdate(String sql,
                           Object[] parameters,
                           final String[] autoGeneratedKeys,
                           StatementCallback<T> callback) {
        try {
            parameters = sanitizeParams(parameters);

            PreparedStatement statement = null;
            final Connection connection = connection();

            try {
                if (autoGeneratedKeys == null || autoGeneratedKeys.length == 0) {
                    statement = connection.prepareStatement(sql);
                } else {
                    statement = connection.prepareStatement(sql, autoGeneratedKeys);
                }

                SqlUtils.setParameters(statement, parameters);

                return callback.run(statement);
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                        // Ignore.
                        throw new PersistException("Error closing prepared statement: " + e.getMessage(), e);
                    }
                }
            }
        } catch (SQLException e) {
            throw new PersistException("Error creating prepared statement for sql [" + sql
                + "] with autoGeneratedKeys " + Arrays.toString(autoGeneratedKeys) + ": " + e.getMessage(), e);
        }
    }

    // --- lists ---

    /**
     * @param sql
     * @param parameters
     * @param callback
     * @param <T>
     * @return
     */
    public <T> T sqlSelect(String sql,
                           Object[] parameters,
                           StatementCallback<T> callback) {
        parameters = sanitizeParams(parameters);
        try {
            PreparedStatement statement = null;
            final Connection connection = connection();

            try {
                statement = connection.prepareStatement(sql);

                if (parameters != null && parameters.length > 0) {
                    SqlUtils.setParameters(statement, parameters);
                }

                return callback.run(statement);
            } finally {
                if (statement != null) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                        // Ignore.
                        throw new PersistException("Error closing prepared statement: " + e.getMessage(), e);
                    }
                }
            }
        } catch (SQLException e) {
            throw new PersistException("Error creating prepared statement for sql [" + sql
                + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Reads a row from the provided {@link java.sql.ResultSet} and converts it
     * to an object instance of the given class.
     * <p/>
     * See {@link SqlUtils#getValue(java.sql.ResultSet, int, Class)} for details on
     * the mappings between ResultSet.get methods and Java types.
     *
     * @param objectClass type of the object to be returned
     * @param resultSet   {@link java.sql.ResultSet} (positioned in the row to be
     *                    processed)
     * @see SqlUtils#getValue(java.sql.ResultSet, int, Class)
     * @since 1.0
     */
    protected <T> T loadObject(final Class<T> objectClass, final ResultSet resultSet) throws SQLException {
        return loadObject(db.findMapping(objectClass), objectClass, resultSet);
    }

    /**
     * Reads a row from the provided {@link java.sql.ResultSet} and converts it
     * to an object instance of the given class.
     * <p/>
     * See {@link SqlUtils#getValue(java.sql.ResultSet, int, Class)} for details on
     * the mappings between ResultSet.get methods and Java types.
     *
     * @param objectClass type of the object to be returned
     * @param resultSet   {@link java.sql.ResultSet} (positioned in the row to be
     *                    processed)
     * @see SqlUtils#getValue(java.sql.ResultSet, int, Class)
     * @since 1.0
     */
    @SuppressWarnings("unchecked")
    protected <T> T loadObject(Mapping mapping, final Class<T> objectClass, final ResultSet resultSet) throws SQLException {
        final ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

        if (mapping == null) {
            throw new PersistException("Mapping not found for class [" + objectClass.getCanonicalName() + "].");
        }

        // for beans
        if (mapping.isNativeType()) {
            return (T) SqlUtils.getValue(resultSet, 1, mapping.getEntityClass());
        }

        T ret;

        try {
            ret = (T) mapping.getFastClass().newInstance();
        } catch (Exception e) {
            throw new PersistException(e);
        }

        for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
            String columnName = resultSetMetaData.getColumnLabel(i);//.toUpperCase();

            if (columnName.equalsIgnoreCase("RN__")) {
                continue;
            }

            final Mapping.Property property = mapping.getProperty(columnName);
            if (property == null) {
                LOG.warn("Column [" + columnName
                    + "] from result set does not have a mapping to a field in ["
                    + objectClass.getName() + "]");
                continue;
            }

            final Class type = property.getType();
            final Object value = SqlUtils.getValue(resultSet, i, type);

            try {
                property.set(ret, value);
            } catch (Exception e) {
                throw new PersistException("Error setting value [" + value + "]"
                    + (value == null ? "" : " of type [" + value.getClass().getName() + "]") + " from column ["
                    + columnName + "] using field [" + property.reflectField.getName() + "]: " + e.getMessage(), e);
            }
        }

        return ret;
    }

    @Deprecated
    public <T> T sqlOne(Class<T> objectClass, SelectQuery query, List<Condition> conditions) {
        if (conditions != null && !conditions.isEmpty()) {
            query.addConditions(conditions);
        }

        return sqlOne(objectClass, query.getSQL(), query.getBindValues());
    }

    /**
     * Reads a single object from the database by mapping the results of the SQL
     * query into an instance of the given object class. Only the columns
     * returned from the SQL query will be set into the object instance. If a
     * given column can't be mapped to the target object instance, a
     * {@link PersistException} will be thrown.
     *
     * @since 1.0
     */
    public <T> T sqlOne(final Class<T> objectClass, final String sql) {
        return sqlOne(objectClass, sql, (Object[]) null);
    }

    // --- iterators ---

    /**
     * Reads a single object from the database by mapping the results of the
     * parameterized SQL query into an instance of the given object class. Only
     * the columns returned from the SQL query will be set into the object
     * instance. If a given column can't be mapped to the target object
     * instance, a {@link PersistException} will be thrown.
     * <p/>
     * Parameters will be set according with the correspondence defined in
     *
     * @since 1.0
     */
    public <T> T sqlOne(final Class<T> objectClass, final String sql, final Object... parameters) {
        return sqlSelect(sql, parameters, statement -> {
            try {
                return sqlOne(objectClass, statement.executeQuery());
            } catch (SQLException e) {
                throw new PersistException(e);
            }
        });
    }

    /**
     * Reads a single object from the database by mapping the content of the
     * ResultSet current row into an instance of the given object class. Only
     * columns contained in the ResultSet will be set into the object instance.
     * If a given column can't be mapped to the target object instance, a
     * RuntimeSQLException will be thrown.
     *
     * @since 1.0
     */
    protected <T> T sqlOne(final Class<T> objectClass, final ResultSet resultSet) {
        T ret = null;
        try {
            if (resultSet.next()) {
                ret = loadObject(objectClass, resultSet);
                if (resultSet.next()) {
                    throw new PersistException("Non-unique result returned.");
                }
            }
        } catch (SQLException e) {
            throw new PersistException(e);
        } finally {
            try {
                resultSet.close();
            } catch (Exception e) {
                // Ignore.
            }
        }

        return ret;
    }

    /**
     * Reads a list of objects from the database by mapping the content of the
     * ResultSet into instances of the given object class. Only columns
     * contained in the ResultSet will be set into the object instances. If a
     * given column can't be mapped to a target object instance, a
     * RuntimeSQLException will be thrown.
     *
     * @since 1.0
     */
    protected <T> List<T> sqlList(final Class<T> objectClass, final ResultSet resultSet) {
        final List<T> ret = Lists.newArrayList();
        try {
            final Mapping mapping = db.findMapping(objectClass);
            while (resultSet.next()) {
                ret.add(loadObject(objectClass, resultSet));
            }
        } catch (SQLException e) {
            throw new PersistException(e);
        } finally {
            try {
                resultSet.close();
            } catch (Exception e) {
                // Ignore.
            }
        }

        return ret;
    }

    /**
     * Reads a list of objects from the database by mapping the results of the
     * execution of the given PreparedStatement into instances of the given
     * object class. Only the columns returned from the PreparedStatement
     * execution will be set into the object instances. If a given column can't
     * be mapped to a target object instance, a RuntimeSQLException will be
     * thrown.
     * <p/>
     * Parameters will be set according with the correspondence defined in
     *
     * @since 1.0
     */
    protected <T> List<T> sqlList(final Class<T> objectClass,
                                  final PreparedStatement statement) {
        try {
            try {
                if (statement.getParameterMetaData().getParameterCount() <= 100) {
                    statement.setFetchSize(100);
//                    if (statement instanceof OracleStatement) {
//                        ((OracleStatement) statement).setRowPrefetch(100);
//                    }
                } else {
                    statement.setFetchSize(255);
//                    if (statement instanceof OracleStatement) {
//                        ((OracleStatement) statement).setRowPrefetch(255);
//                    }
                }
            } catch (Exception ex) {
                try {
                    statement.setFetchSize(100);
                } catch (Exception e) {
                    LOG.error("Could not set fetch size.", e);
                }
            }

            return sqlList(objectClass, statement.executeQuery());
        } catch (SQLException e) {
            throw new PersistException(e);
        }
    }

    /**
     * Reads a list of objects from the database by mapping the results of the
     * SQL query into instances of the given object class. Only the columns
     * returned from the SQL query will be set into the object instance. If a
     * given column can't be mapped to the target object instance, a
     * {@link PersistException} will be thrown.
     *
     * @since 1.0
     */
    public <T> List<T> sqlList(final Class<T> objectClass, final String sql) {
        return sqlList(objectClass, sql, (Object[]) null);
    }

    /**
     * @param objectClass
     * @param query
     * @param conditions
     * @param <T>
     * @return
     */
    public <T> List<T> sqlList(Class<T> objectClass, SelectQuery query, List<Condition> conditions) {
        if (conditions != null && !conditions.isEmpty()) {
            query.addConditions(conditions);
        }

        return sqlList(objectClass, query.getSQL(), query.getBindValues());
    }

    /**
     * Reads a list of objects from the database by mapping the results of the
     * parameterized SQL query into instances of the given object class. Only
     * the columns returned from the SQL query will be set into the object
     * instance. If a given column can't be mapped to the target object
     * instance, a {@link PersistException} will be thrown.
     * <p/>
     * Parameters will be set according with the correspondence defined in
     *
     * @since 1.0
     */
    public <T> List<T> sqlList(final Class<T> objectClass, final String sql, final Object... parameters) {
        return sqlSelect(sql, parameters, statement -> sqlList(objectClass, statement));
    }

    /**
     * Reads a single object from the database by mapping the results of the SQL
     * query into an instance of {@link java.util.Map}.
     * <p/>
     * Types returned from the database will be converted to Java types in the
     * map according with the correspondence defined in
     * {@link SqlUtils#getValue(java.sql.ResultSet, int, int)}.
     *
     * @since 1.0
     */
    public Map<String, Object> sqlMap(final String sql) {
        return sqlMap(sql, (Object[]) null);
    }

    /**
     * Reads a single object from the database by mapping the results of the SQL
     * query into an instance of {@link java.util.Map}.
     * <p/>
     * Types returned from the database will be converted to Java types in the
     * map according with the correspondence defined in
     * {@link SqlUtils#getValue(java.sql.ResultSet, int, int)}.
     * <p/>
     * Parameters will be set according with the correspondence defined in
     *
     * @since 1.0
     */
    public Map<String, Object> sqlMap(final String sql, final Object... parameters) {
        return sqlSelect(sql, parameters, statement -> sqlMap(statement));
    }

    /**
     * Reads a single object from the database by mapping the results of the
     * PreparedStatement execution into an instance of {@link java.util.Map}.
     * <p/>
     * Types returned from the database will be converted to Java types in the
     * map according with the correspondence defined in
     * {@link SqlUtils#getValue(java.sql.ResultSet, int, int)}.
     * <p/>
     * Parameters will be set according with the correspondence defined in
     *
     * @since 1.0
     */
    protected Map<String, Object> sqlMap(final PreparedStatement statement) {
        try {
            try {
                if (statement.getParameterMetaData().getParameterCount() <= 100) {
                    statement.setFetchSize(100);
//                    if (statement instanceof OracleStatement) {
//                        ((OracleStatement) statement).setRowPrefetch(100);
//                    }
                } else {
                    statement.setFetchSize(255);
//                    if (statement instanceof OracleStatement) {
//                        ((OracleStatement) statement).setRowPrefetch(255);
//                    }
                }
            } catch (Exception ex) {
                try {
                    statement.setFetchSize(100);
                } catch (Exception e) {
                    // Do nothing.
                }
            }

            return sqlMap(statement.executeQuery());
        } catch (SQLException e) {
            throw new PersistException(e);
        }
    }

    /**
     * Reads a single object from the database by mapping the results of the
     * current ResultSet row into an instance of {@link java.util.Map}.
     * <p/>
     * Types returned from the database will be converted to Java types in the
     * map according with the correspondence defined in
     * {@link SqlUtils#getValue(java.sql.ResultSet, int, int)}.
     *
     * @since 1.0
     */
    protected Map<String, Object> sqlMap(final ResultSet resultSet) {
        Map<String, Object> ret = null;

        try {
            if (resultSet.next()) {
                ret = SqlUtils.loadMap(resultSet);
                if (resultSet.next()) {
                    throw new PersistException("Non-unique result returned.");
                }
            } else {
                ret = null;
            }
        } catch (SQLException e) {
            throw new PersistException(e);
        } finally {
            try {
                resultSet.close();
            } catch (Exception e) {
            }
        }

        return ret;
    }

    /**
     * Reads a list of objects from the database by mapping the ResultSet rows
     * to instances of {@link java.util.Map}.
     * <p/>
     * Types returned from the database will be converted to Java types in the
     * map according with the correspondence defined in
     * {@link SqlUtils#getValue(java.sql.ResultSet, int, int)}.
     *
     * @since 1.0
     */
    protected List<Map<String, Object>> sqlMapList(final ResultSet resultSet) {
        final List ret = new ArrayList();
        try {
            while (resultSet.next()) {
                ret.add(SqlUtils.loadMap(resultSet));
            }
        } catch (SQLException e) {
            throw new PersistException(e);
        } finally {
            try {
                resultSet.close();
            } catch (Exception e) {
            }
        }

        return ret;
    }

    /**
     * Reads a list of objects from the database by mapping the
     * PreparedStatement execution results to instances of {@link java.util.Map}.
     * <p/>
     * Types returned from the database will be converted to Java types in the
     * map according with the correspondence defined in
     * {@link SqlUtils#getValue(java.sql.ResultSet, int, int)}.
     * <p/>
     * Parameters will be set according with the correspondence defined in
     *
     * @since 1.0
     */
    protected List<Map<String, Object>> sqlMapList(final PreparedStatement statement) {
        try {
            statement.setFetchSize(100);
            return sqlMapList(statement.executeQuery());
        } catch (SQLException e) {
            throw new PersistException(e);
        }
    }

    /**
     * Reads a list of objects from the database by mapping the SQL execution
     * results to instances of {@link java.util.Map}.
     * <p/>
     * Types returned from the database will be converted to Java types in the
     * map according with the correspondence defined in
     * {@link SqlUtils#getValue(java.sql.ResultSet, int, int)}.
     * <p/>
     * Parameters will be set according with the correspondence defined in
     *
     * @since 1.0
     */
    public List<Map<String, Object>> sqlMapList(final String sql, final Object... parameters) {
        return sqlSelect(sql, parameters, statement -> sqlMapList(statement));
    }

    /**
     * Reads a list of all objects in the database mapped to the given object
     * class and return each result as an instance of {@link java.util.Map}.
     * <p/>
     * Types returned from the database will be converted to Java types in the
     * map according with the correspondence defined in
     * {@link SqlUtils#getValue(java.sql.ResultSet, int, int)}.
     *
     * @since 1.0
     */
    public List<Map<String, Object>> sqlMapList(final String sql) {
        return sqlMapList(sql, (Object[]) null);
    }

    /**
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlResult<Integer> store(E entity) {
        Preconditions.checkNotNull(entity, "entity must be specified");
        // Get mapper.
        final EntityMapper<E, UpdatableRecord> mapper = db.entityMapper(entity.getClass());
        // Map to Record.
        final UpdatableRecord record = mapper.map(entity);
        // Attach record.
        attach(record);
        // Store it.
        int result = record.store();
        // Merge into entity.
        mapper.merge(record, entity);
        // Return result.
        return success(result);
    }

    /**
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlResult<Integer> insert(E entity) {
        Preconditions.checkNotNull(entity, "entity must be specified");
        // Get mapper.
        final EntityMapper<E, UpdatableRecord> mapper = db.entityMapper(entity.getClass());
        // Map to Record.
        final UpdatableRecord record = mapper.map(entity);
        // Attach record.
        attach(record);
        // Store it.
        int result = record.insert();
        // Merge into entity.
        mapper.merge(record, entity);
        // Return result.
        return success(result);
    }

    /**
     * @param entities
     * @param <E>
     */
    public <E extends AbstractEntity> SqlResult<int[]> insert(List<E> entities) {
        if (entities == null || entities.isEmpty()) {
            return success(new int[0]);
        }

        final EntityMapper<E, UpdatableRecord> mapper = db.entityMapper(entities);

        final Map<E, UpdatableRecord<?>> recordMap = Maps.newHashMapWithExpectedSize(entities.size());
        final List<UpdatableRecord<?>> records = Lists.newArrayList();
        for (E entity : entities) {
            final UpdatableRecord<?> record = mapper.map(entity);
            records.add(record);
            recordMap.put(entity, record);
        }

        int[] results = create().batchInsert(records).execute();
        for (E entity : entities) {
            final UpdatableRecord<?> record = recordMap.get(entity);
            mapper.merge(record, entity);
        }

        return success(results);
    }

    /**
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlResult<Integer> update(final E entity) {
        Preconditions.checkNotNull(entity, "entity must be specified");
        // Get mapper.
        final EntityMapper<E, UpdatableRecord> mapper = db.entityMapper(entity.getClass());
        // Map to Record.
        final UpdatableRecord record = mapper.map(entity);
        // Attach record.
        attach(record);
        // Store it.
        int result = record.update();
        // Merge into entity.
        mapper.merge(record, entity);
        // Return result.
        return success(result);
    }

    public <E extends AbstractEntity, R extends UpdatableRecord<R>> SqlResult<int[]> update(final List<E> entities) {
        Preconditions.checkNotNull(entities, "entities was null");

        // Ensure list to update isn't empty.
        if (entities.isEmpty()) {
            return success(new int[0]);
        }

        final EntityMapper<E, UpdatableRecord> mapper = db.entityMapper(entities);

        final Map<E, UpdatableRecord<?>> recordMap = Maps.newHashMapWithExpectedSize(entities.size());
        final List<UpdatableRecord<?>> records = Lists.newArrayList();
        for (E entity : entities) {
            final UpdatableRecord<?> record = mapper.map(entity);
            records.add(record);
            recordMap.put(entity, record);
        }

        int[] results = create().batchUpdate(records).execute();
        for (E entity : entities) {
            final UpdatableRecord<?> record = recordMap.get(entity);
            mapper.merge(record, entity);
        }

        return success(results);
    }

    /**
     * @param object
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity, R extends UpdatableRecord<R>> SqlResult<Integer> delete(final E object) {
        Preconditions.checkNotNull(object, "object must be specified");
        final EntityMapper<E, R> mapper = mapper(object.getClass());
        final R record = mapper.map(object);
        attach(record);
        final int ret = record.delete();
        mapper.merge(record, object);
        return success(ret);
    }

    public <E extends AbstractEntity, R extends UpdatableRecord<R>> SqlResult<int[]> delete(final List<E> entities) {
        Preconditions.checkNotNull(entities, "entities was null");

        // Ensure list to update isn't empty.
        if (entities.isEmpty()) {
            return success(new int[0]);
        }

        final EntityMapper<E, UpdatableRecord> mapper = db.entityMapper(entities);

        final Map<E, UpdatableRecord<?>> recordMap = Maps.newHashMapWithExpectedSize(entities.size());
        final List<UpdatableRecord<?>> records = Lists.newArrayList();
        for (E entity : entities) {
            final UpdatableRecord<?> record = mapper.map(entity);
            records.add(record);
            recordMap.put(entity, record);
        }

        int[] results = create().batchDelete(records).execute();
        for (E entity : entities) {
            final UpdatableRecord<?> record = recordMap.get(entity);
            mapper.merge(record, entity);
        }

        return success(results);
    }

    /**
     * @param entityClass
     * @param id
     * @param <T>
     * @return
     */
    public <T extends AbstractEntity> SqlResult<Integer> delete(Class<T> entityClass, String id) {
        final T entity = getEntity(entityClass, id);
        if (entity == null) {
            return success(0);
        }
        return delete(entity);
    }

    public SelectSelectStep<Record> select() {
        return create().select();
    }

    public SelectSelectStep<Record> select(SelectField<?>... fields) {
        return create().select(fields);
    }

    private Object[] sanitizeParams(Object[] parameters) {
        if (parameters == null || parameters.length == 0) {
            return parameters;
        }

        final List<Object> params = Lists.newArrayListWithExpectedSize(parameters.length);
        for (Object param : parameters) {
            // Unwrap Collection.
            if (param instanceof Collection) {
                params.addAll((Collection) param);
            } else if (params instanceof Map) {
                params.addAll(((Map) params).values());
            } else {
                params.add(param);
            }
        }

        return params.toArray(new Object[params.size()]);
    }

    private void saveVersion(TableMapping mapping, UpdatableRecord record) {
        if (mapping == null || !mapping.isJournalingWorking() || mapping.isJournalRecord(record)) {
            return;
        }

        // Insert Journal Record.
        final UpdatableRecord journalRecord = db.journalMapper(mapping).map(record);
        journalRecord.attach(configuration);
        journalRecord.insert();
    }

    private void saveDeleteVersion(TableMapping mapping, UpdatableRecord record) {
        if (mapping == null || !mapping.isJournalingWorking() || mapping.isJournalRecord(record)) {
            return;
        }

        // Insert Journal Record.
        final UpdatableRecord journalRecord = db.journalMapper(mapping).map(record);
        journalRecord.setValue(mapping.JOURNAL_VERSION(), -2L);
        journalRecord.setValue(mapping.JOURNAL_CHANGED(), record.getValue(mapping.CHANGED()));
        journalRecord.attach(configuration);
        journalRecord.insert();
    }

    void updateCache() {
        if (!cachePuts.isEmpty()) {
            cachePuts.keySet().forEach(mapping -> {

            });
        }
        if (!cacheInvalidates.isEmpty()) {
            cacheInvalidates.keySet().forEach(mapping -> {

            });
        }
    }

    private final class RecordListenerImpl implements RecordListener {
        public RecordListenerImpl() {
        }

        @Override
        public void storeStart(RecordContext ctx) {

        }

        @Override
        public void storeEnd(RecordContext ctx) {

        }

        @Override
        public void insertStart(RecordContext ctx) {
            final Record record = ctx.record();
            final TableMapping mapping = db.getMapping(record);
            if (mapping == null) {
                return;
            }

            // Create a new ID if needed.
            final String id = record.getValue(mapping.ID());
            if (id == null) {
                record.setValue(mapping.ID(), UUID.randomUUID().toString().replace("-", ""));
            }

            // Insert into Journal table if necessary.
            saveVersion(mapping, (UpdatableRecord) record);

//            // Cache puts.
//            if (mapping.cache != null) {
//                getCachePuts().put(mapping, db.recordMapper(mapping).map(record));
//            }
        }

        @Override
        public void insertEnd(RecordContext ctx) {
            // Add to journal if necessary.
        }

        @Override
        public void updateStart(RecordContext ctx) {
            final Record record = ctx.record();
            final TableMapping mapping = db.getMapping(record);
            if (mapping == null) {
                return;
            }

            // Insert into Journal table if necessary.
            saveVersion(mapping, (UpdatableRecord) record);

            // Cache puts.
//            if (mapping.cache != null) {
//                getCachePuts().put(mapping, db.recordMapper(mapping).map(record));
//            }
        }

        @Override
        public void updateEnd(RecordContext ctx) {
        }

        @Override
        public void deleteStart(RecordContext ctx) {
            final Record record = ctx.record();
            final TableMapping mapping = db.getMapping(record);
            if (mapping == null) {
                return;
            }

            // Insert into Journal table if necessary.
            saveDeleteVersion(mapping, (UpdatableRecord) record);

//            if (mapping.cache != null) {
//                // Invalidate.
//                getCacheInvalidates().put(mapping, record.getValue(mapping.ID()));
//            }
        }

        @Override
        public void deleteEnd(RecordContext ctx) {
        }

        @Override
        public void loadStart(RecordContext ctx) {

        }

        @Override
        public void loadEnd(RecordContext ctx) {

        }

        @Override
        public void refreshStart(RecordContext ctx) {

        }

        @Override
        public void refreshEnd(RecordContext ctx) {

        }

        @Override
        public void exception(RecordContext ctx) {

        }
    }
}
