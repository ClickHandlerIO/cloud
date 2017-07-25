package move.sql;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.SQLDialect;
import org.jooq.Table;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds mapping data from a given class and a table
 */
@SuppressWarnings("unchecked")
public class TableMapping {

  public static final int COLUMN_LENGTH_ID = 32;
  public static final int COLUMN_LENGTH_ENUM = 64;
  public static final int COLUMN_LENGTH_STRING = 128;

  public final static String ENTITY_SUFFIX = "Entity";
  private static final Logger LOG = LoggerFactory.getLogger(TableMapping.class);

  public final Class entityClass;
  public final FastClass fastClass;
  public final Property[] properties;
  public final Property[] embeddedProperties;
  public final Map<String, Property> columnsMap = new LinkedHashMap<>();
  public final SqlPlatform dbPlatform;

  public final move.sql.Table tableAnnotation;
  public final String tableName;
  public final SqlSchema.DbTable schemaTable;
  public final String[] columns;
  public final Field[] fields;
  public final Table tbl;
  public final Field<String> idField;
  private final RecordMapper<? extends Record, ? extends AbstractEntity> recordMapper;
  private final EntityMapper<? extends AbstractEntity, ? extends Record> entityMapper;
  private final Class recordClass;

  private final List<Property> primaryKey = new ArrayList<>();
  private final List<Property> shardKey = new ArrayList<>();

  private final List<Index> indexes = new ArrayList<>();
  private final Map<String, Index> indexMap = new HashMap<>();
  private Multimap<Property, Index> propertyIndexMap = Multimaps.newMultimap(
      Maps.newHashMap(),
      () -> Lists.newArrayListWithExpectedSize(2));

  protected TableMapping(SqlSchema.DbTable schemaTable,
      final SqlPlatform dbPlatform,
      final Class objectClass,
      final Map<String, Table> jooqMap) {
    this.entityClass = objectClass;
    this.fastClass = FastClass.create(entityClass);
    this.dbPlatform = dbPlatform;

    final List<Property> props = Lists.newArrayList();
    final List<Property> embeddedProperties = Lists.newArrayList();
    final Property[] allProperties = extractFields(entityClass, null, "");

    for (Property property : allProperties) {
      if (property.isEmbedded()) {
        embeddedProperties.add(property);
      } else {
        final String columnName = property.columnName;
        if (columnsMap.containsKey(columnName)) {
          throw new PersistException(
              "Duplicate column names found on '" + entityClass.getCanonicalName()
                  + "' Column Name: " + columnName);
        }

        columnsMap.put(columnName, property);
        props.add(property);
      }
    }

    this.properties = props.toArray(new Property[props.size()]);
    this.embeddedProperties = embeddedProperties.toArray(new Property[embeddedProperties.size()]);

    // Table annotation.
    tableAnnotation = (move.sql.Table) objectClass.getAnnotation(move.sql.Table.class);
    Preconditions.checkNotNull(tableAnnotation, "Entity Class [" +
        objectClass.getCanonicalName() + "] does not have a @Table annotation.");

    tableName = SqlUtils.tableName(objectClass, tableAnnotation.name()).toLowerCase();
    Preconditions.checkState(!tableName.isEmpty(), "Entity Class [" +
        objectClass.getCanonicalName() + "] @Table must specify a value for 'name'.");

    this.schemaTable = schemaTable;

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
        property.setDefaultValue(
            SqlUtils.parseValue(defaultValue, property.getType(), column.nullable));
      } catch (Exception e) {
        // Ignore.
      }

      columnsMap.put(columnName, property);
    }

    columns = extractColumnNames(this.properties);

    final String[] primaryKeys = tableAnnotation.primaryKey();
    if (primaryKeys.length == 0) {
      final Property pkProp = getProperty(AbstractEntity.ID);
      pkProp.primaryKey = true;
      primaryKey.add(pkProp);
    } else {
      for (String pkColumn : primaryKeys) {
        final Property pkProp = getProperty(pkColumn);

        if (pkProp == null) {
          throw new PersistException(
              "Primary Key column '" + pkColumn + "' does not map to any property.");
        }

        pkProp.primaryKey = true;
        primaryKey.add(pkProp);
      }
    }

    final String[] shardKeys = tableAnnotation.shardKey();
    if (shardKeys.length == 0) {
      for (Property pkProp : primaryKey) {
        pkProp.shardKey = true;
      }
    } else {
      for (String shardColumn : shardKeys) {
        final Property shardProp = getProperty(shardColumn);

        if (shardProp == null) {
          throw new PersistException(
              "Shard Key column '" + shardColumn + "' does not map to any property.");
        }

        shardProp.shardKey = true;
        shardKey.add(shardProp);
      }
    }

    {
      this.tbl = jooqMap.get(tableName);

      if (this.tbl == null) {
        this.fields = null;
        this.idField = null;
        this.recordMapper = null;
        this.entityMapper = null;
        this.recordClass = null;
        return;
      }

      this.recordClass = this.tbl.getRecordType();

      // Map jOOQ fields to properties.
//            final Field<?>[] jooqFields = this.tbl.fields();
      final Map<String, Field> jooqFields = Arrays
          .stream(this.tbl.fields())
          .collect(Collectors.toMap($ -> $.getName(), Function.identity()));

      jooqFields.forEach((k, v) -> {
        final String columnName = Strings.nullToEmpty(v.getName()).trim().toLowerCase();
        final Property property = columnsMap.get(columnName);
        if (property == null) {
          throw new PersistException("jOOQ Table [" + this.tbl.getClass().getCanonicalName() +
              "] specified field [" + columnName
              + "] that does not correspond to any fields on Entity [" +
              objectClass.getCanonicalName() + "]. Run Schema Generation to synchronize jOOQ.");
        }
        property.jooqField = v;
      });

//            for (Field jooqField : jooqFields) {
//                final String columnName = Strings.nullToEmpty(jooqField.getName()).trim().toLowerCase();
//                final Property property = columnsMap.get(columnName);
//                if (property == null) {
//                    throw new PersistException("jOOQ Table [" + this.tbl.getClass().getCanonicalName() +
//                        "] specified field [" + columnName + "] that does not correspond to any fields on Entity [" +
//                        objectClass.getCanonicalName() + "]. Run Schema Generation to synchronize jOOQ.");
//                }
//                property.jooqField = jooqField;
//            }

      this.recordMapper = record -> {
        final AbstractEntity entity;
        try {
          entity = (AbstractEntity) fastClass.newInstance();
        } catch (InvocationTargetException e) {
          throw new PersistException("Failed to create an instance of class [" +
              entityClass.getCanonicalName() + "]", e);
        }

        for (int i = 0; i < this.properties.length; i++) {
          final Property property = this.properties[i];
          try {
            // Get value from record.
            Object value = record.getValue(property.jooqField);
            // Handle Enum Properties.
            if (value != null && property.enumType && value instanceof String) {
              property.set(entity, property.enumConstantMap.get((String) value));
            } else if (value != null && value instanceof ZonedDate) {
              property.set(entity, ((ZonedDate) value).getUtc());
            } else {
              property.set(entity, value);
            }
          } catch (Exception e) {
            throw new PersistException("Failed to set field [" + property.columnName +
                "] on an instance of class [" + entityClass.getCanonicalName() + "]", e);
          }
        }
        return entity;
      };

      this.entityMapper = new EntityMapper<AbstractEntity, Record>() {
        @Override
        public UpdatableRecord map(AbstractEntity entity) {
          final UpdatableRecord<?> record = (UpdatableRecord<?>) tbl.newRecord();

          for (int i = 0; i < TableMapping.this.properties.length; i++) {
            final Property property = TableMapping.this.properties[i];
            try {
              final Object value = property.get(entity);
              // Handle enum types.
              if (value != null && property.enumType) {
                record.setValue(property.jooqField, value.toString());
              } else if (value != null && value instanceof LocalDate) {
                record.setValue(property.jooqField, java.sql.Date.valueOf((LocalDate) value));
              } else if (value != null && value instanceof LocalTime) {
                record.setValue(property.jooqField, java.sql.Time.valueOf((LocalTime) value));
              } else if (value != null && value instanceof LocalDateTime) {
                record.setValue(property.jooqField, Timestamp.valueOf((LocalDateTime) value));
              } else if (value != null && value instanceof ZonedDateTime) {
                record.setValue(property.jooqField,
                    Timestamp.from(((ZonedDateTime) value).toInstant()));
              } else if (value != null && value instanceof Duration) {
                record.setValue(property.jooqField, ((Duration) value).toNanos());
              } else if (value != null && value instanceof Instant) {
                record.setValue(property.jooqField, ((Instant) value).toEpochMilli());
              } else {
                record.setValue(property.jooqField, value);
              }
            } catch (Exception e) {
              throw new PersistException("Failed to set field [" + property.columnName +
                  "] on an instance of class [" + entityClass.getCanonicalName() + "]", e);
            }
          }
          return record;
        }

        @Override
        public void merge(Record record, AbstractEntity intoEntity) {
          for (int i = 0; i < TableMapping.this.properties.length; i++) {
            final Property property = TableMapping.this.properties[i];
            try {
              property.set(intoEntity, record.getValue(property.jooqField));
            } catch (Exception e) {
              throw new PersistException("Failed to set field [" + property.columnName +
                  "] on an instance of class [" + entityClass.getCanonicalName() + "]", e);
            }
          }
        }
      };

      this.fields = jooqFields.values().toArray(new Field[jooqFields.size()]);
      this.idField = field(String.class, AbstractEntity.ID);
    }

    Indexes indexes = (Indexes) entityClass.getAnnotation(Indexes.class);
    if (indexes != null && indexes.value().length > 0) {
      for (final move.sql.Index indexAnnotation : indexes.value()) {
        final IndexColumn[] columns = indexAnnotation.columns();
        if (columns.length == 0) {
          throw new PersistException("@Index on [" + entityClass.getCanonicalName() +
              "] does not specify any columns");
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

          name = name + getTableName() + "_" + Joiner.on("_").join(columnNames);
        }

        final SqlSchema.DbIndex dbIndex =
            schemaTable != null ? schemaTable.indexes.get(name) : null;

        final IndexProperty[] indexProperties = new IndexProperty[columns.length];
        for (int i = 0; i < columns.length; i++) {
          final IndexColumn column = columns[i];
          final Property property = columnsMap.get(column.value());
          if (property == null) {
            throw new PersistException(
                "@Index on [" + entityClass.getCanonicalName() + "] with @IndexColumn(" + column
                    .value() + ") does not map to an actual column on the table.");
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
            indexAnnotation.unique(),
            indexAnnotation.clustered(),
            columnNames,
            dbIndex,
            indexProperties
        );

        this.indexMap.put(index.name, index);
        this.indexes.add(index);

        for (IndexProperty indexProperty : indexProperties) {
          indexProperty.index = index;
          this.propertyIndexMap.put(indexProperty.property, index);
        }
      }
    }
  }

  public static TableMapping create(final SqlSchema.DbTable schemaTable,
      final SqlPlatform dbPlatform,
      final Class entityClass,
      final Map<String, Table> jooqMap) {
    return new TableMapping(schemaTable, dbPlatform, entityClass, jooqMap);
  }

  public Field<String> ID() {
    return idField;
  }

  public boolean hasColumnName(String columnName) {
    return columnsMap.containsKey(columnName);
  }

  public Table TBL() {
    return tbl;
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
      throw new PersistException("Type [" + entityClass.getCanonicalName()
          + "] does not have a generated jOOQ table. Run jOOQ Generator to sync jOOQ with Entities!");
    }

    for (Property property : properties) {
      if (!property.isMapped()) {
        throw new PersistException(
            "Class [" + entityClass.getCanonicalName() + "] field [" + property.name
                + "] mapped to column [" + property.columnName
                + "] which does not exist. Run jOOQ Generator to sync jOOQ with Entities!");
      }
      if (property.jooqField == null) {
        throw new PersistException(
            "Class [" + entityClass.getCanonicalName() + "] field [" + property.name
                + "] mapped to column [" + property.columnName
                + "] does not map to a JOOQ Field. Run jOOQ Generator to sync jOOQ with Entities!");
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

  public String getTableName() {
    return tableName;
  }

  public DSLContext create(Configuration configuration) {
    return DSL.using(configuration);
  }

  public <R extends Record, E extends AbstractEntity> RecordMapper<R, E> getRecordMapper() {
    return (RecordMapper<R, E>) recordMapper;
  }

  public <E extends AbstractEntity, R extends Record> EntityMapper<E, R> getEntityMapper() {
    return (EntityMapper<E, R>) entityMapper;
  }

  public List<Property> getPrimaryKeyProperties() {
    return primaryKey;
  }

  public List<Property> getShardKeyProperties() {
    return shardKey;
  }

  public List<Index> getIndexes() {
    return Collections.unmodifiableList(indexes);
  }

  public Index getIndex(String name) {
    return indexMap.get(name);
  }

  public Class<? extends AbstractEntity> getEntityClass() {
    return entityClass;
  }

  public FastClass getFastClass() {
    return fastClass;
  }

  public Property[] getProperties() {
    return properties;
  }

  public Property getProperty(String columnName) {
    return columnsMap.get(Strings.nullToEmpty(columnName).trim().toLowerCase());
  }

  public <T> T copy(T obj) {
    T toObj;
    try {
      toObj = (T) fastClass.newInstance();
    } catch (InvocationTargetException e) {
      return null;
    }

    merge(obj, toObj);

    return toObj;
  }

  public <T> void merge(T from, T to) {
    for (Property property : getProperties()) {
      try {
        property.set(to, property.get(from));
      } catch (Exception e) {
        // Ignore.
      }
    }
  }

  protected String getterName(String fieldName, Class type) {
    fieldName = Strings.nullToEmpty(fieldName).trim();
    if (fieldName.isEmpty()) {
      throw new PersistException("Invalid FieldName");
    }
    // Capitalize first letter.
    fieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

    return type == boolean.class || type == Boolean.class
        ? "is" + fieldName
        : "get" + fieldName;
  }

  protected String setterName(String fieldName) {
    fieldName = Strings.nullToEmpty(fieldName).trim();
    if (fieldName.isEmpty()) {
      throw new PersistException("Invalid FieldName");
    }

    return "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
  }

  protected void collectMethods(Map<String, Method> map, Class cls) {
    if (cls == null || cls == Object.class) {
      return;
    }
    final Method[] methods = cls.getDeclaredMethods();
    if (methods != null) {
      for (Method method : methods) {
        map.put(method.getName(), method);
      }
    }
    collectMethods(map, cls.getSuperclass());
  }

  protected Property[] extractFields(Class objectClass, Property parent, String prefix) {
    final List<Property> properties = new ArrayList<>();

    final Class superClass = objectClass.getSuperclass();
    if (superClass != null && superClass != Object.class) {
      final Property[] parentProperties = extractFields(superClass, null, prefix);
      if (parentProperties != null && parentProperties.length != 0) {
        properties.addAll(Lists.newArrayList(parentProperties));
      }
    }

    final java.lang.reflect.Field[] declaredFields = objectClass.getDeclaredFields();
    if (declaredFields == null || declaredFields.length == 0) {
      return properties.toArray(new Property[properties.size()]);
    }

    final String columnNamePrefix = Strings.nullToEmpty(prefix).trim();
    final FastClass objectFastClass = FastClass.create(objectClass);

    final Map<String, Method> methodMap = new HashMap<>();
    collectMethods(methodMap, objectClass);

    for (java.lang.reflect.Field field : declaredFields) {
      if (Modifier.isStatic(field.getModifiers())) {
        continue;
      }

      // Ignore no column.
      if (field.getDeclaredAnnotation(NoColumn.class) != null) {
        continue;
      }

      final Column columnAnnotation = field.getDeclaredAnnotation(Column.class);
      // Ensure @Column annotation.
      if (columnAnnotation == null) {
        throw new PersistException(
            "Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName()
                + "] does not have a @Column annotation.");
      }

      final String getterName = getterName(field.getName(), field.getType());
      Method getter = methodMap.get(getterName);

      if (getter == null && getterName.startsWith("is")) {
        getter = methodMap.get("get" + getterName.substring(2));
      }
      if (getter == null) {
        throw new PersistException(
            "Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName()
                + "] does not have a getter [" + getterName + "].");
      }
      if (!Modifier.isPublic(getter.getModifiers())) {
        throw new PersistException(
            "Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName()
                + "] getter [" + getterName + "] must be public.");
      }
      if (Modifier.isStatic(getter.getModifiers())) {
        throw new PersistException(
            "Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName()
                + "] getter [" + getterName + "] cannot be static.");
      }
      if (getter.getParameterCount() != 0) {
        throw new PersistException(
            "Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName()
                + "] getter [" + getterName + "] cannot have any parameters.");
      }
      if (getter.getReturnType() != field.getType()) {
        throw new PersistException(
            "Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName()
                + "] getter [" + getterName + "] have type mismatch [" + field.getType()
                .getCanonicalName() + "] != [" + getter.getReturnType().getCanonicalName() + "].");
      }

      final String setterName = setterName(field.getName());
      final Method setter = methodMap.get(setterName);
      if (setter == null) {
        throw new PersistException(
            "Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName()
                + "] does not have a setter [" + setterName + "]");
      }
      if (!Modifier.isPublic(setter.getModifiers())) {
        throw new PersistException(
            "Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName()
                + "] getter [" + setterName + "] must be public.");
      }
      if (Modifier.isStatic(setter.getModifiers())) {
        throw new PersistException(
            "Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName()
                + "] getter [" + setterName + "] cannot be static.");
      }
      if (setter.getParameterCount() != 1) {
        throw new PersistException(
            "Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName()
                + "] setter [" + setterName + "] must have exactly 1 parameter.");
      }
      if (setter.getParameterTypes()[0] != field.getType()) {
        throw new PersistException(
            "Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName()
                + "] setter [" + setterName + "] have type mismatch [" + field.getType()
                .getCanonicalName() + "] != [" + setter.getParameterTypes()[0].getCanonicalName()
                + "].");
      }
      if (setter.getReturnType() != void.class) {
        throw new PersistException(
            "Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName()
                + "] setter [" + setterName + "] cannot return anything.");
      }

      final Property property = new Property();
      field.setAccessible(true);
      property.reflectField = field;
      property.name = field.getName();
      property.type = field.getType();
      property.nullable = columnAnnotation.nullable();
      if (property.nullable && property.type.isPrimitive()) {
        property.nullable = false;
      }
      property.columnAnnotation = columnAnnotation;
      property.getter = FastClass.create(getter.getDeclaringClass()).getMethod(getter);
      property.setter = FastClass.create(setter.getDeclaringClass()).getMethod(setter);
      property.dbType = columnAnnotation.dbType();
      property.parentType = columnAnnotation.parentType();
      property.autoGenerated = columnAnnotation.autoGenerated();
      property.enumType = property.type.isEnum();
      if (property.enumType) {
        property.enumConstants = property.type.getEnumConstants();
        property.enumConstantMap = new HashMap<>();
        if (property.enumConstants != null) {
          for (Object value : property.enumConstants) {
            property.enumConstantMap.put(value.toString(), value);
          }
        }
      }
      property.embedded = !SqlUtils.isNativeType(property.type) && !property.type.isEnum();
      property.columnName = SqlUtils
          .columnName(columnNamePrefix, columnAnnotation, property.name, property.embedded);
      property.prefix = columnNamePrefix;

      if (!property.type.isPrimitive()) {
        property.fastClass = FastClass.create(property.type);
      }

      property.parent = parent;

      // Set Column Length.
      property.columnLength = columnAnnotation.length();
      if (property.columnLength == 0
          && (property.type == String.class || property.enumType)) {
        if (property.enumType) {
          int largest = 0;
          if (property.enumConstants != null) {
            for (Object obj : property.enumConstants) {
              int l = obj.toString().length();
              largest = largest < l ? l : largest;
            }
          }
          property.columnLength = largest + 10;
        } else if (property.columnName.toLowerCase().endsWith("_id") || property.columnName
            .toLowerCase().equals("id")) {
          property.columnLength = COLUMN_LENGTH_ID;
        } else {
          property.dbType = DBTypes.LONGVARCHAR;
//                    property.columnLength = COLUMN_LENGTH_STRING;
        }
      }

      property.dbType = property.toDBType();

      // Handle embedded types.
      // Embedded types are essentially unwrapped and flattened out.
      // This supports nested embedding.
      if (property.embedded) {
        final Property[] children = extractFields(property.type, property, property.columnName);
        if (children == null || children.length == 0) {
          throw new PersistException(
              "Class [" + objectClass.getCanonicalName() + "] specified an embedded field of type ["
                  + property.type.getCanonicalName() + "] which has no fields specified.");
        }
        properties.addAll(Lists.newArrayList(children));
      } else {
        properties.add(property);
      }
    }

    return properties.toArray(new Property[properties.size()]);
  }

  /**
   *
   */
  public static class Index {

    public final TableMapping mapping;
    public final String name;
    public final boolean unique;
    public final boolean clustered;
    public final String[] columnNames;
    public final IndexProperty[] properties;
    public final SqlSchema.DbIndex dbIndex;

    public Index(TableMapping mapping,
        String name,
        boolean unique,
        boolean clustered,
        String[] columnNames,
        SqlSchema.DbIndex dbIndex,
        IndexProperty[] properties) {
      this.mapping = mapping;
      this.name = name;
      this.unique = unique;
      this.clustered = clustered;
      this.columnNames = columnNames;
      this.dbIndex = dbIndex;
      this.properties = properties;
    }
  }

  /**
   *
   */
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

  /**
   *
   */
  public class Property {

    public boolean enumType;
    public Class parentType;
    public FastMethod getter;
    public FastMethod setter;
    protected java.lang.reflect.Field reflectField;
    protected Property parent;
    protected String prefix;
    protected Class type;
    protected String name;
    protected String columnName;
    protected FastClass fastClass;
    protected boolean embedded;
    protected boolean autoGenerated;
    protected int dbType;
    protected SqlSchema.DbColumn column;
    protected Object defaultValue;
    protected Field jooqField;
    protected Object[] enumConstants;
    protected Map<String, Object> enumConstantMap;
    private int columnLength;
    private boolean nullable;
    private boolean primaryKey;
    private boolean shardKey;
    public Column columnAnnotation;

    public Property getParent() {
      return parent;
    }

    public String getName() {
      return name;
    }

    public Class getType() {
      return type;
    }

    public String getColumnName() {
      return columnName;
    }

    public boolean isMapped() {
      return column != null;
    }

    public boolean isAutoGenerated() {
      return autoGenerated;
    }

    public boolean isEmbedded() {
      return embedded;
    }

    public boolean isPrimaryKey() {
      return primaryKey;
    }

    public boolean isShardKey() {
      return shardKey;
    }

    public int getDbType() {
      return dbType;
    }

    public SqlSchema.DbColumn getColumn() {
      return column;
    }

    public Object getDefaultValue() {
      return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
      this.defaultValue = defaultValue;
    }

    public Object get(Object obj) throws InvocationTargetException, IllegalAccessException {
      Object value = getter.invoke(getParentInstance(obj), null);

      if (value != null && value instanceof String && columnLength > 0) {
        final String val = (String) value;
        if (val.length() > columnLength) {
          return val.substring(0, columnLength - 1);
        }
      }

      return value;
    }

    public void set(Object obj, Object value)
        throws InvocationTargetException, IllegalAccessException {
      if (value != null) {
        if (enumType && value instanceof String) {
          final Object enumConstant = enumConstantMap.get(value);
          setter.invoke(getParentInstance(obj), new Object[]{enumConstant});
        } else if (type == LocalDateTime.class && value instanceof Timestamp) {
          setter
              .invoke(getParentInstance(obj), new Object[]{((Timestamp) value).toLocalDateTime()});
        } else if (type == LocalDate.class && value instanceof Timestamp) {
          setter.invoke(getParentInstance(obj),
              new Object[]{((Timestamp) value).toLocalDateTime().toLocalDate()});
        } else if (type == LocalDate.class && value instanceof java.sql.Date) {
          setter.invoke(getParentInstance(obj),
              new Object[]{(((java.sql.Date) value).toLocalDate())});
        } else if (type == LocalTime.class && value instanceof java.sql.Time) {
          setter.invoke(getParentInstance(obj),
              new Object[]{(((java.sql.Time) value).toLocalTime())});
        } else if (type == LocalTime.class && value instanceof java.sql.Timestamp) {
          setter.invoke(getParentInstance(obj),
              new Object[]{(((java.sql.Timestamp) value).toLocalDateTime().toLocalTime())});
        } else if (type == ZonedDateTime.class && value instanceof Timestamp) {
          setter.invoke(getParentInstance(obj),
              new Object[]{ZonedDateTime.ofInstant(((Timestamp) value).toInstant(),
                  ZoneId.systemDefault())});
        } else if (type == Duration.class && value instanceof Long) {
          setter.invoke(getParentInstance(obj), new Object[]{Duration.ofNanos((Long) value)});
        } else if (type == Instant.class && value instanceof Long) {
          setter.invoke(getParentInstance(obj), new Object[]{Instant.ofEpochMilli((Long) value)});
        } else {
          setter.invoke(getParentInstance(obj), new Object[]{value});
        }
      } else {
        setter.invoke(getParentInstance(obj), new Object[]{value});
      }
    }

    public Object getParentInstance(Object root)
        throws InvocationTargetException, IllegalAccessException {
      if (parent == null) {
        return root;
      }

      return parent.getInstance(root);
    }

    public Object getOrCreate(Object parent)
        throws InvocationTargetException, IllegalAccessException {
      Object current = reflectField.get(parent);
      if (current == null) {
        current = fastClass.newInstance();
        reflectField.set(parent, current);
      }
      return current;
    }

    public Object getInstance(Object root)
        throws InvocationTargetException, IllegalAccessException {
      if (parent == null) {
        return getOrCreate(root);
      }

      final Object parentInstance = parent.getInstance(root);
      return getOrCreate(parentInstance);
    }

    public int getColumnLength() {
      return columnLength;
    }

    public boolean isNullable() {
      return nullable;
    }

    public void setNullable(boolean nullable) {
      this.nullable = nullable;
    }

    public DataType columnDataType() {
      if (column == null) {
        return null;
      }

      DataType dataType = dbPlatform.fromJdbcType(column.dataType, this);
      if (dataType == null) {
        return null;
      }

      dataType = dataType.nullable(column.nullable);
      dataType = dataType.length(column.columnSize);

      return dataType;
    }

    public DataType fieldDataType() {
      DataType dataType = dbPlatform.fromJdbcType(dbType, this);
      if (dataType == null) {
        return null;
      }

      boolean enumType = false;
      if (dataType == NuoDBDataType.ENUM) {
        enumType = true;
        String val = "('" + Joiner.on("','").join(enumConstants) + "')";
        dataType = new DefaultDataType<>(SQLDialect.MYSQL,
            new DefaultDataType<>(null, String.class, "enum " + val), "enum", "enum " + val);
      }

      dataType = dataType.nullable(nullable);

      if (columnLength > 0 && !dataType.getTypeName().equalsIgnoreCase("string") && !enumType) {
        dataType = dataType.length(columnLength);
      }

      if (!dataType.nullable() || getType().isPrimitive()) {
        final Class type = getType();

        if (type == byte.class || type == Byte.class) {
          dataType = dataType.defaultValue((byte) 0);
        } else if (type == boolean.class || type == Boolean.class) {
          dataType = dataType.defaultValue(false);
        } else if (type == short.class || type == Short.class) {
          dataType = dataType.defaultValue((short) 0);
        } else if (type == int.class || type == Integer.class) {
          dataType = dataType.defaultValue(0);
        } else if (type == long.class || type == Long.class) {
          dataType = dataType.defaultValue(0L);
        } else if (type == float.class || type == Float.class) {
          dataType = dataType.defaultValue(0.0f);
        } else if (type == double.class || type == Double.class) {
          dataType = dataType.defaultValue(0.0);
        } else if (type == String.class) {
          dataType = dataType.defaultValue("");
        } else if (type == byte[].class) {
          dataType = dataType.defaultValue(new byte[0]);
        } else if (type == Date.class) {
          dataType = dataType.defaultValue(0L);
        } else if (type.isEnum()) {
          dataType = dataType.defaultValue("");
        }
      }

      return dataType;
    }

    public int toDBType() {
      final int dbType = getDbType();
      if (dbType != 0) {
        return dbType;
      }

      final Class type = getType();

      if (type == byte.class || type == Byte.class) {
        return DBTypes.TINYINT;
      }

      if (type == boolean.class || type == Boolean.class) {
        return DBTypes.BOOLEAN;
      }

      if (type == short.class || type == Short.class) {
        return DBTypes.SMALLINT;
      }

      if (type == int.class || type == Integer.class) {
        return DBTypes.INTEGER;
      }

      if (type == long.class || type == Long.class) {
        return DBTypes.BIGINT;
      }

      if (type == float.class || type == Float.class) {
        return DBTypes.FLOAT;
      }

      if (type == double.class || type == Double.class) {
        return DBTypes.DOUBLE;
      }

      if (type == String.class) {
        return DBTypes.VARCHAR;
      }

      if (type == byte[].class) {
        return DBTypes.VARBINARY;
      }

      if (type == Date.class) {
        return DBTypes.TIMESTAMP;
      }

      if (type == LocalDateTime.class) {
        return DBTypes.TIMESTAMP;
      }

      if (type == ZonedDateTime.class) {
        return DBTypes.TIMESTAMP;
      }

      if (type == LocalDate.class) {
        return DBTypes.DATE;
      }

      if (type == LocalTime.class) {
        return DBTypes.TIME;
      }

      if (type == Duration.class) {
        return DBTypes.BIGINT;
      }

      if (type == Instant.class) {
        return DBTypes.BIGINT;
      }

      if (type.isEnum()) {
        return DBTypes.ENUM;
      }

      return dbType;
    }
  }
}
