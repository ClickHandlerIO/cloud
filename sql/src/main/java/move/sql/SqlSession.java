package move.sql;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import move.common.UID;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.JooqUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Session to a SQL database using a single Connection.
 */
@SuppressWarnings("all")
public class SqlSession {
    private static final Logger LOG = LoggerFactory.getLogger(SqlSession.class);
    private static final SqlResult success = SqlResult.success();
    private static final SqlResult rollback = SqlResult.rollback();
    protected final SqlDatabase db;
    protected final Stack<Configuration> configurationStack = new Stack<>();
    private Configuration configuration;

    SqlSession(final SqlDatabase db,
               final Configuration configuration) {
        this.db = db;
        this.configuration = configuration.derive();
    }

    public SqlResult commit() {
        return success;
    }

    public <T> SqlResult<T> commit(T result) {
        return SqlResult.commit(result);
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
    public <R extends Record> R record(Class<R> recordClass) {
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
    public <R extends Record> R attach(R record) {
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
    public <R extends Record> void attach(Collection<R> records) {
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
    public <R extends Record> void attach(List<R> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (int i = 0; i < records.size(); i++) {
            records.get(i).attach(configuration);
        }
    }

    /**
     * @param entity
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends Record> R map(E entity) {
        Preconditions.checkNotNull(entity);
        final EntityMapper<E, R> mapper = db.entityMapper(entity.getClass());
        Preconditions.checkNotNull(mapper);
        return mapper.map(entity);
    }

    /**
     * @param cls
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends Record> EntityMapper<E, R> mapper(Class cls) {
        Preconditions.checkNotNull(cls);
        return db.entityMapper(cls);
    }

    /**
     * @param entities
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends Record> List<R> mapRecords(Collection<E> entities) {
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
    public <E extends AbstractEntity, R extends Record> List<R> mapRecords(EntityMapper<E, R> mapper, Collection<E> entities) {
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
    public <R extends Record> R getRecord(Class<R> recordClass, String id) {
        return getRecord(db.getMapping(recordClass), id);
    }

    public <R extends Record> R newRecord(Class<R> recordClass) {
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
    public <R extends Record> R getRecordOrNew(Class<R> recordClass, String id) {
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
    public <R extends Record> R getRecord(TableMapping mapping, String id) {
        org.jooq.Table tbl = mapping.TBL();//.as("a");
        return (R) create().selectFrom(tbl).where(tbl.field(mapping.ID()).eq(id)).fetchOne();
    }

    /**
     * @param recordClass
     * @param id
     * @param <R>
     * @return
     */
    public <R extends Record> List<R> getRecords(Class<R> recordClass, String... id) {
        return getRecords(recordClass, Lists.newArrayList(id));
    }

    /**
     * @param recordClass
     * @param ids
     * @param <R>
     * @return
     */
    public <R extends Record> List<R> getRecords(Class<R> recordClass, Collection<String> ids) {
        return getRecords(db.getMapping(recordClass), ids);
    }

    /**
     * @param mapping
     * @param ids
     * @param <R>
     * @return
     */
    public <R extends Record> List<R> getRecords(TableMapping mapping, Collection<String> ids) {
        return getRecords(mapping, ids, 255);
    }

    /**
     * @param mapping
     * @param collection
     * @param <R>
     * @return
     */
    public <R extends Record> List<R> getRecords(final TableMapping mapping,
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

        // Copy and io.clickhandler.action.store original sort order.
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
    public <R extends Record> void getRecords(Class cls, Map<String, R> map) {
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
    protected <R extends Record> void fetch(TableMapping mapping, List<R> recordList, Collection<String> ids) {
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
    protected <R extends Record> List<R> fetch(TableMapping mapping, Collection<String> ids) {
        return (List<R>) create().selectFrom(mapping.TBL()).where(mapping.ID().in(ids)).fetch();
    }

    /**
     * @param cls
     * @param id
     * @param <E>
     * @return
     */
    public <R extends Record, E extends AbstractEntity> E getEntity(Class<E> cls, String id) {
        return db.map((R) getRecord(db.getCheckedMapping(cls), id));
    }

    /**
     * @param entityClass
     * @param ids
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends Record> List<E> getEntities(Class<E> entityClass, String... ids) {
        return getEntities(entityClass, Lists.newArrayList(ids));
    }

    /**
     * @param entityClass
     * @param ids
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends Record> List<E> getEntities(Class<E> entityClass, Collection<String> ids) {
        return db.map((List<R>) getRecords(db.getMapping(entityClass), ids));
    }

    /**
     * @param entityClass
     * @param ids
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends Record> Map<String, E> getMap(Class<E> entityClass, Collection<String> ids) {
        final List<E> entities = db.map((List<R>) getRecords(db.getMapping(entityClass), ids));
        if (entities == null || entities.isEmpty())
            return Collections.emptyMap();
        return entities.stream().collect(Collectors.toMap(k -> k.getId(), Function.identity()));
    }

    /**
     * @param entityClass
     * @param ids
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends Record> Map<String, E> getMap(Class<E> entityClass, Map<String, E> toMap, Collection<String> ids) {
        if (toMap == null)
            toMap = new HashMap<>();

        final List<E> entities = db.map((List<R>) getRecords(db.getMapping(entityClass), ids));
        if (entities == null || entities.isEmpty())
            return toMap;

        final Map<String, E> m = toMap;
        entities.forEach(entity -> m.put(entity.getId(), entity));
        return toMap;
    }

    /**
     * @param entityClass
     * @param condition
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity> Map<String, E> selectMap(Class<E> entityClass, Condition condition, int limit) {
        final List<E> entities = select(entityClass, condition, limit);
        if (entities == null || entities.isEmpty())
            return Collections.emptyMap();
        return entities.stream().collect(Collectors.toMap(k -> k.getId(), Function.identity()));
    }

    /**
     * @param entityClass
     * @param map
     * @param <E>
     */
    public <E extends AbstractEntity> void getEntities(Class<E> entityClass, Map<String, E> map) {
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

    /**
     * @param cls
     * @return
     */
    public SelectWhereStep<?> selectFrom(Class cls) {
        final TableMapping mapping = db.getCheckedMapping(cls);
        return create().selectFrom(mapping.TBL());
    }

    /**
     * @return
     */
    public SelectSelectStep<Record> select() {
        return create().select();
    }

    /**
     * @param fields
     * @return
     */
    public SelectSelectStep<Record> select(SelectField<?>... fields) {
        return create().select(fields);
    }

    /**
     * @param cls
     * @param condition
     * @param <T>
     * @return
     */
    public <T> List<T> select(final Class<T> cls, Condition condition) {
        return select(cls, condition, 1000);
    }

    /**
     * @param cls
     * @param condition
     * @param limit
     * @param <T>
     * @return
     */
    public <T> List<T> select(final Class<T> cls, Condition condition, int limit) {
        final TableMapping mapping = db.getMapping(cls);
        if (mapping == null) {
            throw new RuntimeException("No mapping for class [" + cls.getCanonicalName() + "]");
        }
        return create().selectFrom(mapping.tbl).where(condition).limit(limit).fetchInto(cls);
    }

    /**
     * @param cls
     * @param conditions
     * @param <T>
     * @return
     */
    public <T> List<T> select(final Class<T> cls, Collection<? extends Condition> conditions) {
        return select(cls, conditions, 1000);
    }

    /**
     * @param cls
     * @param condition
     * @param limit
     * @param <T>
     * @return
     */
    public <T> List<T> select(final Class<T> cls, Collection<? extends Condition> conditions, int limit) {
        final TableMapping mapping = db.getMapping(cls);
        if (mapping == null) {
            throw new RuntimeException("No mapping for class [" + cls.getCanonicalName() + "]");
        }
        return create().selectFrom(mapping.tbl).where(conditions).limit(limit).fetchInto(cls);
    }

    /**
     * @param cls
     * @param condition
     * @param limit
     * @param <T>
     * @return
     */
    public <T> List<T> select(final Class<T> cls, Collection<? extends Condition> conditions, int limit, int timeoutInSeconds) {
        final TableMapping mapping = db.getMapping(cls);
        if (mapping == null) {
            throw new RuntimeException("No mapping for class [" + cls.getCanonicalName() + "]");
        }
        return create().selectFrom(mapping.tbl).where(conditions).limit(limit).queryTimeout(timeoutInSeconds).fetchInto(cls);
    }

    /**
     * @param cls
     * @param condition
     * @param <T>
     * @return
     */
    public <T> T selectOne(final Class<T> cls, Condition condition) {
        final TableMapping mapping = db.getMapping(cls);
        if (mapping == null) {
            throw new RuntimeException("No mapping for class [" + cls.getCanonicalName() + "]");
        }
        return (T) create().selectFrom(mapping.tbl).where(condition).limit(1).fetchOneInto(cls);
    }

    /**
     * @param cls
     * @param conditions
     * @param <T>
     * @return
     */
    public <T> T selectOne(final Class<T> cls, Collection<? extends Condition> conditions) {
        final TableMapping mapping = db.getMapping(cls);
        if (mapping == null) {
            throw new RuntimeException("No mapping for class [" + cls.getCanonicalName() + "]");
        }
        return (T) create().selectFrom(mapping.tbl).where(conditions).limit(1).fetchOneInto(cls);
    }

    /**
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> Query insertQuery(E entity) {
        Preconditions.checkNotNull(entity, "entity must be specified");
        final TableMapping mapping = db.getCheckedMapping(entity.getClass());

        if (entity.getId() == null || entity.getId().isEmpty()) {
            entity.setId(UID.next());
        }

        try {
            final List<Field<?>> fields = new ArrayList<>(mapping.getProperties().length);
            final List<Object> values = new ArrayList<>(mapping.getProperties().length);

            for (TableMapping.Property property : mapping.getProperties()) {
                final Object value = property.get(entity);
                if (value != null) {
                    fields.add(property.jooqField);
                    values.add(value);
                }
            }

            return create().insertInto(mapping.tbl).columns(fields).values(values);
        } catch (Throwable e) {
            Throwables.propagate(e);
            return null;
        }
    }

    /**
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlResult<Integer> insert(E entity) {
        Preconditions.checkNotNull(entity, "entity must be specified");
        return SqlResult.success(insertQuery(entity).queryTimeout(4).execute());
    }

    /**
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlResult<Integer> insertAtomic(E entity) {
        Preconditions.checkNotNull(entity, "entity must be specified");
        return SqlResult.atomic(insertQuery(entity).execute());
    }

    /**
     * @param entities
     * @param <E>
     */
    public <E extends AbstractEntity> SqlResult<int[]> insert(List<E> entities) {
        return insert(entities, 30);
    }

    /**
     * @param entities
     * @param <E>
     */
    public <E extends AbstractEntity> SqlResult<int[]> insert(List<E> entities, int timeoutSeconds) {
        if (entities == null || entities.isEmpty()) {
            return success(new int[0]);
        }

        return SqlResult.success(
            JooqUtils.execute(
                configuration,
                timeoutSeconds,
                entities.stream().map(this::insertQuery).collect(Collectors.toList())
            )
        );
    }

    /**
     * @param entities
     * @param <E>
     */
    public <E extends AbstractEntity> SqlResult<int[]> insertAtomic(List<E> entities) {
        return insertAtomic(entities, 30);
    }

    /**
     * @param entities
     * @param <E>
     */
    public <E extends AbstractEntity> SqlResult<int[]> insertAtomic(List<E> entities, int timeoutSeconds) {
        if (entities == null || entities.isEmpty()) {
            return success(new int[0]);
        }

        return SqlResult.atomic(
            JooqUtils.execute(
                configuration,
                timeoutSeconds,
                entities.stream().map(this::insertQuery).collect(Collectors.toList())
            )
        );
    }

    /**
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> Query updateQuery(E entity) {
        return updateQuery(entity, null);
    }

    /**
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> Query updateQuery(E entity, Condition condition) {
        Preconditions.checkNotNull(entity, "entity must be specified");
        final TableMapping mapping = db.getCheckedMapping(entity.getClass());

        if (entity.getId() == null || entity.getId().isEmpty()) {
            throw new RuntimeException("Entity [" + entity.getClass().getCanonicalName() + "] does not have it's 'id' set. Cannot update.");
        }

        try {
            UpdateSetStep query = create().update(mapping.tbl);
            UpdateSetMoreStep last = null;

            for (TableMapping.Property property : mapping.getProperties()) {
                // Do not update Primary Key and/or Shard Key columns.
                if (property.isPrimaryKey() || property.isShardKey())
                    continue;
                final Object value = property.get(entity);
                last = query.set(property.jooqField, value);
            }

            if (last == null) {
                throw new RuntimeException("Entity [" + entity.getClass().getCanonicalName() + "] has no fields to create.");
            }

            Condition primaryKeyCondition = null;
            if (mapping.getPrimaryKeyProperties().size() > 0) {
                for (TableMapping.Property prop : mapping.getPrimaryKeyProperties()) {
                    if (primaryKeyCondition != null) {
                        primaryKeyCondition = primaryKeyCondition.and(prop.jooqField.eq(prop.get(entity)));
                    } else {
                        primaryKeyCondition = prop.jooqField.eq(prop.get(entity));
                    }
                }
            } else {
                primaryKeyCondition = mapping.idField.eq(entity.getId());
            }

            if (condition == null)
                return last.where(primaryKeyCondition);
            else
                return last.where(primaryKeyCondition.and(condition));
        } catch (Throwable e) {
            Throwables.propagate(e);
            return null;
        }
    }

    /**
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlResult<Integer> update(final E entity) {
        Preconditions.checkNotNull(entity, "entity must be specified");
        return SqlResult.success(updateQuery(entity).execute());
    }

    /**
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlResult<Integer> updateAtomic(final E entity) {
        Preconditions.checkNotNull(entity, "entity must be specified");
        return SqlResult.atomic(updateQuery(entity).execute());
    }

    /**
     * @param entity
     * @param condition
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlResult<Integer> update(final E entity, Condition condition) {
        Preconditions.checkNotNull(entity, "entity must be specified");
        return SqlResult.success(updateQuery(entity, condition).execute());
    }

    /**
     * @param entity
     * @param condition
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> SqlResult<Integer> updateAtomic(final E entity, Condition condition) {
        Preconditions.checkNotNull(entity, "entity must be specified");
        return SqlResult.atomic(updateQuery(entity, condition).execute());
    }

    /**
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity, R extends Record> SqlResult<Integer> update(final E entity, Collection<? extends Field<?>> fields) {
        Preconditions.checkNotNull(entity, "entity must be specified");
        return SqlResult.success(updateQuery(entity).execute());
    }

    /**
     * @param entity
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity, R extends Record> SqlResult<Integer> updateAtomic(final E entity, Collection<? extends Field<?>> fields) {
        Preconditions.checkNotNull(entity, "entity must be specified");
        return SqlResult.atomic(updateQuery(entity).execute());
    }

    /**
     * @param entities
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends UpdatableRecord<R>> SqlResult<int[]> update(final List<E> entities) {
        return update(entities, 30);
    }

    /**
     * @param entities
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends UpdatableRecord<R>> SqlResult<int[]> update(final List<E> entities, int timeoutSeconds) {
        Preconditions.checkNotNull(entities, "entities was null");

        // Ensure list to update isn't empty.
        if (entities.isEmpty()) {
            return success(new int[0]);
        }

        return SqlResult.success(
            JooqUtils.execute(
                configuration,
                timeoutSeconds,
                entities.stream().map(this::updateQuery).collect(Collectors.toList())
            )
        );
    }

    /**
     * @param entities
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends UpdatableRecord<R>> SqlResult<int[]> updateAtomic(final List<E> entities) {
        return updateAtomic(entities, 30);
    }

    /**
     * @param entities
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends UpdatableRecord<R>> SqlResult<int[]> updateAtomic(final List<E> entities, int timeoutSeconds) {
        Preconditions.checkNotNull(entities, "entities was null");

        // Ensure list to update isn't empty.
        if (entities.isEmpty()) {
            return success(new int[0]);
        }

        return SqlResult.atomic(
            JooqUtils.execute(
                configuration,
                timeoutSeconds,
                entities.stream().map(this::updateQuery).collect(Collectors.toList())
            )
        );
    }

    /**
     * @param entityClass
     * @param id
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> DeleteConditionStep deleteQuery(final Class<E> entityClass,
                                                                      String id) {
        return deleteQuery(entityClass, id, null);
    }

    /**
     * @param entityClass
     * @param id
     * @param condition
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> DeleteConditionStep deleteQuery(final Class<E> entityClass,
                                                                      String id,
                                                                      Condition condition) {
        Preconditions.checkNotNull(entityClass, "entityClass must be specified");
        Preconditions.checkNotNull(id, "id must be specified");

        final TableMapping mapping = db.getCheckedMapping(entityClass);
        if (condition == null)
            return create().deleteFrom(mapping.tbl).where(mapping.idField.eq(id));
        else
            return create().deleteFrom(mapping.tbl).where(mapping.idField.eq(id).and(condition));
    }

    /**
     * @param object
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> DeleteConditionStep deleteQuery(final E object) {
        return deleteQuery(object, null);
    }

    /**
     * @param object
     * @param condition
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity> DeleteConditionStep deleteQuery(final E object, Condition condition) {
        Preconditions.checkNotNull(object, "object must be specified");
        return deleteQuery(object.getClass(), object.getId(), condition);
    }

    /**
     * @param object
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity, R extends Record> SqlResult<Integer> delete(final E object) {
        Preconditions.checkNotNull(object, "object must be specified");
        return SqlResult.success(deleteQuery(object).execute());
    }

    /**
     * @param object
     * @param <E>
     * @return
     */
    public <E extends AbstractEntity, R extends Record> SqlResult<Integer> deleteAtomic(final E object) {
        Preconditions.checkNotNull(object, "object must be specified");
        return SqlResult.atomic(deleteQuery(object).execute());
    }

    /**
     * @param entities
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends UpdatableRecord<R>> SqlResult<int[]> delete(final List<E> entities) {
        return delete(entities, 30);
    }

    /**
     * @param entities
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends UpdatableRecord<R>> SqlResult<int[]> delete(final List<E> entities, int timeoutSeconds) {
        Preconditions.checkNotNull(entities, "entities was null");

        // Ensure list to delete isn't empty.
        if (entities.isEmpty()) {
            return success(new int[0]);
        }

        return SqlResult.success(
            JooqUtils.execute(
                configuration,
                timeoutSeconds,
                entities.stream().map(this::deleteQuery).collect(Collectors.toList())
            )
        );
    }

    /**
     * @param entities
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends UpdatableRecord<R>> SqlResult<int[]> deleteAtomic(final List<E> entities) {
        return deleteAtomic(entities, 30);
    }

    /**
     * @param entities
     * @param <E>
     * @param <R>
     * @return
     */
    public <E extends AbstractEntity, R extends UpdatableRecord<R>> SqlResult<int[]> deleteAtomic(final List<E> entities, int timeoutSeconds) {
        Preconditions.checkNotNull(entities, "entities was null");

        // Ensure list to delete isn't empty.
        if (entities.isEmpty()) {
            return success(new int[0]);
        }

        return SqlResult.atomic(
            JooqUtils.execute(
                configuration,
                timeoutSeconds,
                entities.stream().map(this::deleteQuery).collect(Collectors.toList())
            )
        );
    }

    /**
     * @param entityClass
     * @param id
     * @param <T>
     * @return
     */
    public <T extends AbstractEntity> SqlResult<Integer> delete(Class<T> entityClass, String id) {
        Preconditions.checkNotNull(entityClass, "entityClass was null");
        return SqlResult.success(deleteQuery(entityClass, id).execute());
    }

    /**
     * @param entityClass
     * @param id
     * @param <T>
     * @return
     */
    public <T extends AbstractEntity> SqlResult<Integer> deleteAtomic(Class<T> entityClass, String id) {
        Preconditions.checkNotNull(entityClass, "entityClass was null");
        return SqlResult.atomic(deleteQuery(entityClass, id).execute());
    }

    /**
     * @param queries
     * @return
     */
    public Batch batch(Collection<? extends Query> queries) {
        return create().batch(queries);
    }

    public SqlBatch batch() {
        return new SqlBatch(this);
    }

    /**
     * @param queries
     * @return
     */
    public SqlResult<int[]> save(Stream<? extends Query> queries) {
        return save(queries.collect(Collectors.toList()));
    }

    /**
     * @param queries
     * @return
     */
    public SqlResult<int[]> saveAtomic(Stream<? extends Query> queries) {
        return saveAtomic(queries.collect(Collectors.toList()), 30);
    }

    /**
     * @param queries
     * @return
     */
    public SqlResult<int[]> save(Collection<? extends Query> queries) {
        return save(queries, 30);
    }

    /**
     * @param queries
     * @return
     */
    public SqlResult<int[]> save(Collection<? extends Query> queries, int timeoutSeconds) {
        return SqlResult.success(
            JooqUtils.execute(
                configuration,
                timeoutSeconds,
                queries
            )
        );
    }

    /**
     * @param queries
     * @return
     */
    public SqlResult<int[]> saveAtomic(Collection<? extends Query> queries) {
        return saveAtomic(queries, 30);
    }

    /**
     * @param queries
     * @return
     */
    public SqlResult<int[]> saveAtomic(Collection<? extends Query> queries, int timeoutSeconds) {
        return SqlResult.atomic(
            JooqUtils.execute(
                configuration,
                timeoutSeconds,
                queries
            )
        );
    }

    /**
     * @param queries
     * @return
     */
    public SqlResult<int[]> save(List<? extends Query> queries, Logger logger) {
        return save(queries, 30, logger);
    }

    /**
     * @param queries
     * @return
     */
    public SqlResult<int[]> save(List<? extends Query> queries, int timeoutSeconds, Logger logger) {
        final SqlResult<int[]> result = SqlResult.success(
            JooqUtils.execute(
                configuration,
                timeoutSeconds,
                queries
            )
        );
        if (result.isRollback()) {
            if (logger.isDebugEnabled()) {
                SqlUtils.failedQueries(queries, result.get()).forEach(s -> logger.debug("Failed to Commit: " + s));
            }
        }
        return result;
    }

    /**
     * @param queries
     * @return
     */
    public SqlResult<int[]> saveAtomic(List<? extends Query> queries, Logger logger) {
        return saveAtomic(queries, 30, logger);
    }

    /**
     * @param queries
     * @return
     */
    public SqlResult<int[]> saveAtomic(List<? extends Query> queries, int timeoutSeconds, Logger logger) {
        final SqlResult<int[]> result = SqlResult.atomic(
            JooqUtils.execute(
                configuration,
                timeoutSeconds,
                queries
            )
        );
        if (result.isRollback()) {
            if (logger.isDebugEnabled()) {
                SqlUtils.failedQueries(queries, result.get()).forEach(s -> logger.debug("Failed to Commit: " + s));
            }
        }
        return result;
    }
}
