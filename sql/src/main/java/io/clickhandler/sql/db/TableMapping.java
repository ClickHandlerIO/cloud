package io.clickhandler.sql.db;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import io.clickhandler.sql.entity.AbstractEntity;
import io.clickhandler.sql.annotations.IndexColumn;
import io.clickhandler.sql.annotations.Indexes;
import net.sf.cglib.reflect.FastClass;
import org.jooq.*;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Holds mapping data from a given class and a table
 */
@SuppressWarnings("unchecked")
public class TableMapping extends Mapping {
    public final static String JOURNAL_TABLE_SUFFIX = "_jnl";
    public final static String ENTITY_SUFFIX = "Entity";
    private static final Logger LOG = LoggerFactory.getLogger(TableMapping.class);

    public final io.clickhandler.sql.annotations.Table tableAnnotation;
    public final String tableName;
    public final String journalTableName;
    public final SqlSchema.DbTable schemaTable;
    public final SqlSchema.DbTable schemaJournalTable;
    public final String[] columns;
    public final Field[] fields;
    public final Table tbl;
    public final Table journalTbl;
    public final Field<Long> journalVersionField;
    public final Field<Date> journalChangedField;
    private final Map<Property, Relationship> parentMap = Maps.newHashMap();
    private final Map<Property, Relationship> childrenMap = Maps.newHashMap();
    private final Field<String> idField;
    private final Field<Long> versionField;
    private final Field<Date> changedField;
    private final RecordMapper<? extends UpdatableRecord, ? extends AbstractEntity> recordMapper;
    private final RecordMapper<? extends UpdatableRecord, ? extends UpdatableRecord> journalMapper;
    private final EntityMapper<? extends AbstractEntity, ? extends UpdatableRecord> entityMapper;
    private final Class recordClass;
    private final Class journalRecordClass;

    private final List<Index> indexes = new ArrayList<>();
    private final List<Index> journalIndexes = new ArrayList<>();
    private final Map<String, Index> indexMap = new HashMap<>();
    private final Map<String, Index> journalIndexMap = new HashMap<>();
    private Multimap<Property, Index> propertyIndexMap = Multimaps.newMultimap(
            Maps.newHashMap(),
            () -> Lists.newArrayListWithExpectedSize(2));

    protected TableMapping(final SqlSchema.DbTable schemaTable,
                           final SqlSchema.DbTable schemaJournalTable,
                           final DatabasePlatform dbPlatform,
                           final Class objectClass,
                           final Map<String, Table> jooqMap) {
        super(objectClass, dbPlatform);

        // Table annotation.
        tableAnnotation = (io.clickhandler.sql.annotations.Table) objectClass.getAnnotation(io.clickhandler.sql.annotations.Table.class);
        Preconditions.checkNotNull(tableAnnotation, "Entity Class [" + objectClass.getCanonicalName() + "] does not have a @Table annotation.");

        tableName = tableName(objectClass, tableAnnotation.name(), false).toLowerCase();
        Preconditions.checkState(!tableName.isEmpty(), "Entity Class [" + objectClass.getCanonicalName() + "] @Table must specify a value for 'name'.");

        this.schemaTable = schemaTable;
        this.schemaJournalTable = schemaJournalTable;
        this.journalTableName = tableAnnotation.journal() ? tableName(objectClass, tableName, true) : "";

        // Ensure columnsMap is empty.
        columnsMap.clear();
        for (Property property : properties) {
            final String columnName = Strings.nullToEmpty(property.columnName).trim().toLowerCase();
            SqlSchema.DbColumn column = schemaTable == null ? null : schemaTable.getColumn(columnName);

            if (column == null) {
                columnsMap.put(columnName, property);
                continue;
            }

            property.column = column;
            String defaultValue = column.defaultValue;
            try {
                property.setDefaultValue(SqlUtils.parseValue(defaultValue, property.getType(), column.nullable));
            } catch (Exception e) {
                // Ignore.
            }

            columnsMap.put(columnName, property);
        }

        columns = extractColumnNames(properties);

        {
            this.tbl = jooqMap.get(tableName);

            if (this.tbl == null) {
                this.journalTbl = null;
                this.fields = null;
                this.idField = null;
                this.versionField = null;
                this.changedField = null;
                this.journalVersionField = null;
                this.journalChangedField = null;
                this.recordMapper = null;
                this.entityMapper = null;
                this.journalMapper = null;
                this.recordClass = null;
                this.journalRecordClass = null;
                return;
            }

            this.recordClass = this.tbl.getRecordType();

            // Map jOOQ fields to properties.
            final Field<?>[] jooqFields = this.tbl.newRecord().fields();
            for (Field jooqField : jooqFields) {
                final String columnName = Strings.nullToEmpty(jooqField.getName()).trim().toLowerCase();
                final Property property = columnsMap.get(columnName);
                if (property == null) {
                    throw new PersistException("jOOQ Table [" + this.tbl.getClass().getCanonicalName() + "] specified field [" + columnName + "] that does not correspond to any fields on Entity [" + objectClass.getCanonicalName() + "]. Run Schema Generation to synchronize jOOQ.");
                }
                property.jooqField = jooqField;
            }

            this.recordMapper = record -> {
                final AbstractEntity entity;
                try {
                    entity = (AbstractEntity) fastClass.newInstance();
                } catch (InvocationTargetException e) {
                    throw new PersistException("Failed to create an instance of class [" + entityClass.getCanonicalName() + "]", e);
                }

                for (int i = 0; i < properties.length; i++) {
                    final Property property = properties[i];
                    try {
                        // Get value from record.
                        Object value = record.getValue(property.jooqField);
                        // Handle Enum Properties.
                        if (value != null && property.enumType && value instanceof String) {
                            property.set(entity, property.enumConstantMap.get((String) value));
                        } else {
                            property.set(entity, value);
                        }
                    } catch (Exception e) {
                        throw new PersistException("Failed to set field [" + property.columnName + "] on an instance of class [" + entityClass.getCanonicalName() + "]", e);
                    }
                }
                return entity;
            };

            this.entityMapper = new EntityMapper<AbstractEntity, UpdatableRecord>() {
                @Override
                public UpdatableRecord map(AbstractEntity entity) {
                    final UpdatableRecord<?> record = (UpdatableRecord<?>) tbl.newRecord();

                    for (int i = 0; i < properties.length; i++) {
                        final Property property = properties[i];
                        try {
                            final Object value = property.get(entity);
                            // Handle enum types.
                            if (value != null && property.enumType) {
                                record.setValue(property.jooqField, value.toString());
                            } else {
                                record.setValue(property.jooqField, value);
                            }
                        } catch (Exception e) {
                            throw new PersistException("Failed to set field [" + property.columnName + "] on an instance of class [" + entityClass.getCanonicalName() + "]", e);
                        }
                    }
                    return record;
                }

                @Override
                public void merge(UpdatableRecord record, AbstractEntity intoEntity) {
                    for (int i = 0; i < properties.length; i++) {
                        final Property property = properties[i];
                        try {
                            property.set(intoEntity, record.getValue(property.jooqField));
                        } catch (Exception e) {
                            throw new PersistException("Failed to set field [" + property.columnName + "] on an instance of class [" + entityClass.getCanonicalName() + "]", e);
                        }
                    }
                }
            };

            this.fields = jooqFields;

            this.idField = field(String.class, AbstractEntity.ID);
            this.versionField = field(Long.class, AbstractEntity.VERSION);
            this.changedField = field(Date.class, AbstractEntity.CHANGED);


            if (schemaJournalTable != null) {
                this.journalTbl = jooqMap.get(journalTableName);

                if (this.journalTbl != null) {
                    this.journalRecordClass = journalTbl.getRecordType();

                    Field<Long> jnlVersionField = null;
                    Field<Date> jnlChangedField = null;

                    // Map jOOQ fields to properties.
                    for (Field<?> field : this.journalTbl.fields()) {
                        final String columnName = Strings.nullToEmpty(field.getName()).trim().toLowerCase();
                        final Property property = columnsMap.get(columnName);
                        if (property == null) {
                            throw new PersistException("jOOQ Table [" + journalTbl.getClass().getCanonicalName() + "] specified field [" + columnName + "] that does not correspond to any fields on Entity [" + objectClass.getCanonicalName() + "]. Run Schema Generation to synchronize jOOQ.");
                        }
                        property.jooqJournalField = field;

                        if (columnName.equals(AbstractEntity.VERSION)) {
                            jnlVersionField = (Field<Long>) field;
                        } else if (columnName.equals(AbstractEntity.CHANGED)) {
                            jnlChangedField = (Field<Date>) field;
                        }
                    }

                    this.journalChangedField = jnlChangedField;
                    this.journalVersionField = jnlVersionField;

                    this.journalMapper = record -> {
                        final UpdatableRecord journalRecord = (UpdatableRecord) journalTbl.newRecord();
                        for (Property property : properties) {
                            journalRecord.setValue(property.jooqJournalField, record.getValue(property.jooqField));
                        }
                        return journalRecord;
                    };
                } else {
                    this.journalRecordClass = null;
                    this.journalMapper = null;
                    this.journalChangedField = null;
                    this.journalVersionField = null;
                }
            } else {
                this.journalTbl = null;
                this.journalRecordClass = null;
                this.journalMapper = null;
                this.journalChangedField = null;
                this.journalVersionField = null;
            }
        }

        Indexes indexes = (Indexes) entityClass.getAnnotation(Indexes.class);
        if (indexes != null && indexes.value() != null && indexes.value().length > 0) {
            for (final io.clickhandler.sql.annotations.Index indexAnnotation : indexes.value()) {
                final IndexColumn[] columns = indexAnnotation.columns();
                if (columns.length == 0) {
                    throw new PersistException("@Index on [" + entityClass.getCanonicalName() + "] does not specify any columns");
                }

                String name = Strings.nullToEmpty(indexAnnotation.name()).trim();

                final String[] columnNames = new String[columns.length];
                for (int i = 0; i < columns.length; i++) {
                    columnNames[i] = Strings.nullToEmpty(columns[i].value()).trim();
                }

                if (name.isEmpty()) {
                    if (indexAnnotation.unique()) {
                        name = "ux_";
                    } else {
                        name = "ix_";
                    }

                    name = name + getTableName(indexAnnotation.journal()) + "_" + Joiner.on("_").join(columnNames);
                }

                final SqlSchema.DbIndex dbIndex;
                if (indexAnnotation.journal()) {
                    dbIndex = schemaJournalTable != null ? schemaJournalTable.indexes.get(name) : null;
                } else {
                    dbIndex = schemaTable != null ? schemaTable.indexes.get(name) : null;
                }

                final IndexProperty[] indexProperties = new IndexProperty[columns.length];
                for (int i = 0; i < columns.length; i++) {
                    final IndexColumn column = columns[i];
                    final Property property = columnsMap.get(column.value());
                    if (property == null) {
                        throw new PersistException("@Index on [" + entityClass.getCanonicalName() + "] with @IndexColumn(" + column.value() + ") does not map to an actual column on the table.");
                    }

                    columnNames[i] = property.columnName;

                    SqlSchema.DbIndexColumn dbIndexColumn = null;
                    if (dbIndex != null) {
                        for (SqlSchema.DbIndexColumn dbic : dbIndex.columns) {
                            if (Strings.nullToEmpty(dbic.columnName).trim().equals(property.columnName)) {
                                dbIndexColumn = dbic;
                                break;
                            }
                        }
                    }

                    indexProperties[i] = new IndexProperty(property, column.asc(), dbIndexColumn);
                }

                final Index index = new Index(
                        this,
                        name,
                        indexAnnotation.journal(),
                        indexAnnotation.unique(),
                        indexAnnotation.clustered(),
                        columnNames,
                        dbIndex,
                        indexProperties
                );

                if (indexAnnotation.journal()) {
                    this.journalIndexMap.put(index.name, index);
                    this.journalIndexes.add(index);
                } else {
                    this.indexMap.put(index.name, index);
                    this.indexes.add(index);
                }

                for (IndexProperty indexProperty : indexProperties) {
                    indexProperty.index = index;
                    this.propertyIndexMap.put(indexProperty.property, index);
                }
            }
        }
    }

    public static String tableName(Class entityClass, String name, boolean journal) {
        name = Strings.nullToEmpty(name).trim().toLowerCase();

        // Implicitly create TABLE NAME using this convention (SomeSpecialEntity = some_special)
        if (name.isEmpty()) {
            String entityClassName = entityClass.getSimpleName();
            if (entityClassName.endsWith(ENTITY_SUFFIX)) {
                entityClassName = entityClassName.substring(0, entityClassName.length() - ENTITY_SUFFIX.length());
            }

            final StringBuilder sb = new StringBuilder();
            for (char c : entityClassName.toCharArray()) {
                if (Character.isUpperCase(c)) {
                    if (sb.length() > 0) {
                        sb.append('_');
                    }
                }

                sb.append(Character.toLowerCase(c));
            }

            name = sb.toString();
        }

        if (journal) {
            return name + JOURNAL_TABLE_SUFFIX;
        }

        return name;
    }

    public static TableMapping create(final SqlSchema.DbTable schemaTable,
                                      final SqlSchema.DbTable schemaJournalTable,
                                      final DatabasePlatform dbPlatform,
                                      final Class entityClass,
                                      final Map<String, Table> jooqMap) {
        return new TableMapping(schemaTable, schemaJournalTable, dbPlatform, entityClass, jooqMap);
    }

    public String getTableName(boolean journal) {
        return journal ? journalTableName : tableName;
    }

    public Field<String> ID() {
        return idField;
    }

    public Field<Long> VERSION() {
        return versionField;
    }

    public Field<Long> JOURNAL_VERSION() {
        return journalVersionField;
    }

    public Field<Date> JOURNAL_CHANGED() {
        return journalChangedField;
    }

    public Field<Date> CHANGED() {
        return changedField;
    }

    public boolean hasColumnName(String columnName) {
        return columnsMap.containsKey(columnName);
    }

    public Table TBL() {
        return tbl;
    }

    public boolean isJournalRecord(Record record) {
        return record.getClass() == journalRecordClass;
    }

    @SuppressWarnings("unchecked")
    public <T> Field<T> field(Class<T> cls, String name) {
        final Property property = columnsMap.get(name);
        if (property == null) {
            return null;
        }

        return (Field<T>) property.jooqField;
    }

    public <T> Field<T> field(String name) {
        final Property property = columnsMap.get(name);
        if (property == null) {
            return null;
        }

        return property.jooqField;
    }

    public void checkValidity() {
        if (tbl == null) {
            throw new PersistException("Type [" + entityClass.getCanonicalName() + "] does not have a generated jOOQ table. Run jOOQ Generator to sync jOOQ with Entities!");
        }

        if (tableAnnotation.journal() && journalTbl == null) {
            throw new PersistException("Type [" + entityClass.getCanonicalName() + "] does not have a generated jOOQ table for the Journal. Run jOOQ Generator to sync jOOQ with Entities!");
        }

        for (Property property : properties) {
            if (!property.isMapped()) {
                throw new PersistException("Class [" + entityClass.getCanonicalName() + "] field [" + property.name + "] mapped to column [" + property.columnName + "] which does not exist. Run jOOQ Generator to sync jOOQ with Entities!");
            }
            if (property.jooqField == null) {
                throw new PersistException("Class [" + entityClass.getCanonicalName() + "] field [" + property.name + "] mapped to column [" + property.columnName + "] does not map to a JOOQ Field. Run jOOQ Generator to sync jOOQ with Entities!");
            }
            if (isJournalingWorking() && property.jooqJournalField == null) {
                throw new PersistException("Class [" + entityClass.getCanonicalName() + "] field [" + property.name + "] mapped to column [" + property.columnName + "] does not map to a JOOQ Journal Field. Run jOOQ Generator to sync jOOQ with Entities!");
            }
        }
    }

    private String[] extractColumnNames(Property[] properties) {
        String[] columnNames = new String[properties.length];
        for (int i = 0; i < properties.length; i++) {
            columnNames[i] = properties[i].columnName;
        }
        return columnNames;
    }

    public FastClass getFastClass() {
        return fastClass;
    }

    public String getTableName() {
        return tableName;
    }

    public Property[] getProperties() {
        return properties;
    }

    public DSLContext create(Configuration configuration) {
        return DSL.using(configuration);
    }

    public SelectSelectStep selectAll(Configuration configuration) {
        return create(configuration).select(fields);
    }

    void addChildRelationship(Relationship relationship) {
        childrenMap.put(relationship.foreignKey, relationship);
    }

    void wireRelationships(Map<Class, TableMapping> mappings) {
        for (Property property : properties) {
            final Class parentType = property.parentType;
            if (parentType == null || parentType == Object.class) {
                continue;
            }

            final TableMapping mapping = mappings.get(parentType);

            if (mapping == null) {
                throw new PersistException(getEntityClass().getCanonicalName() + "." + property.getName() +
                        " specified parentType=" + parentType.getCanonicalName() +
                        " which was not found in the mappings map. Make sure it is in the correct package so that it can be properly located.");
            }

            parentMap.put(property, new Relationship(property, mapping));
            mapping.addChildRelationship(new Relationship(property, mapping));
        }
    }

    public <R extends UpdatableRecord<R>, E extends AbstractEntity> RecordMapper<R, E> getRecordMapper() {
        return (RecordMapper<R, E>) recordMapper;
    }

    public RecordMapper<UpdatableRecord, UpdatableRecord> getJournalMapper() {
        return (RecordMapper<UpdatableRecord, UpdatableRecord>) journalMapper;
    }

    public <E extends AbstractEntity, R extends UpdatableRecord<R>> EntityMapper<E, R> getEntityMapper() {
        return (EntityMapper<E, R>) entityMapper;
    }

    public List<Property> getPrimaryKeyProperties(boolean journal) {
        if (journal) {
            return Lists.newArrayList(getProperty(AbstractEntity.ID), getProperty(AbstractEntity.VERSION));
        }
        return Lists.newArrayList(getProperty(AbstractEntity.ID));
    }

    public boolean isJournaling() {
        return tableAnnotation.journal();
    }

    public boolean isJournalingWorking() {
        return journalTbl != null;
    }

    public List<Index> getIndexes() {
        return Collections.unmodifiableList(indexes);
    }

    public List<Index> getJournalIndexes() {
        return Collections.unmodifiableList(journalIndexes);
    }

    public Index getIndex(String name) {
        return indexMap.get(name);
    }

    public Index getJournalIndex(String name) {
        return journalIndexMap.get(name);
    }

    public static class Relationship {
        final Property foreignKey;
        final TableMapping mapping;

        public Relationship(Property foreignKey, TableMapping mapping) {
            this.foreignKey = foreignKey;
            this.mapping = mapping;
        }
    }

    public static class Index {
        public final Mapping mapping;
        public final String name;
        public final boolean journal;
        public final boolean unique;
        public final boolean clustered;
        public final String[] columnNames;
        public final IndexProperty[] properties;
        public final SqlSchema.DbIndex dbIndex;

        public Index(TableMapping mapping,
                     String name,
                     boolean journal,
                     boolean unique,
                     boolean clustered,
                     String[] columnNames,
                     SqlSchema.DbIndex dbIndex,
                     IndexProperty[] properties) {
            this.mapping = mapping;
            this.name = name;
            this.journal = journal;
            this.unique = unique;
            this.clustered = clustered;
            this.columnNames = columnNames;
            this.dbIndex = dbIndex;
            this.properties = properties;
        }
    }

    public static class IndexProperty {
        public final Property property;
        public final boolean asc;
        public final SqlSchema.DbIndexColumn dbIndexColumn;
        private Index index;

        public IndexProperty(Property property, boolean asc, SqlSchema.DbIndexColumn dbIndexColumn) {
            this.property = property;
            this.asc = asc;
            this.dbIndexColumn = dbIndexColumn;
        }

        public Property getProperty() {
            return property;
        }

        public boolean isAsc() {
            return asc;
        }

        public Index getIndex() {
            return index;
        }
    }
}
