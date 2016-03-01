package io.clickhandler.sql;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.sf.cglib.reflect.FastClass;
import net.sf.cglib.reflect.FastMethod;
import org.jooq.DataType;
import org.jooq.Field;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 *
 */
public class Mapping {
    public static final int COLUMN_LENGTH_ID = 32;
    public static final int COLUMN_LENGTH_ENUM = 64;
    public static final int COLUMN_LENGTH_STRING = 128;

    public final Class entityClass;
    public final FastClass fastClass;
    public final boolean nativeType;
    public final Property[] properties;
    public final Property[] embeddedProperties;
    public final Map<String, Property> columnsMap = new LinkedHashMap<>();
    public final SqlPlatform dbPlatform;

    public Mapping(Class entityClass, SqlPlatform dbPlatform) {
        this.entityClass = entityClass;
        this.nativeType = SqlUtils.isNativeType(entityClass);
        this.fastClass = FastClass.create(entityClass);
        this.dbPlatform = dbPlatform;

        if (!isNativeType()) {
            final List<Property> properties = Lists.newArrayList();
            final List<Property> embeddedProperties = Lists.newArrayList();
            final Property[] allProperties = extractFields(entityClass, null, "");

            for (Property property : allProperties) {
                if (property.isEmbedded()) {
                    embeddedProperties.add(property);
                } else {
                    final String columnName = property.columnName;
                    if (columnsMap.containsKey(columnName)) {
                        throw new PersistException("Duplicate column names found on '" + entityClass.getCanonicalName() + "' Column Name: " + columnName);
                    }

                    columnsMap.put(columnName, property);
                    properties.add(property);
                }
            }

            this.properties = properties.toArray(new Property[properties.size()]);
            this.embeddedProperties = embeddedProperties.toArray(new Property[embeddedProperties.size()]);
        } else {
            this.properties = new Property[0];
            this.embeddedProperties = new Property[0];
        }
    }

    public static String columnName(String prefix, Column column, String propertyName, boolean embedded) {
        prefix = Strings.nullToEmpty(prefix).trim().toLowerCase();

        String annotatedName = column == null
            ? ""
            : Strings.nullToEmpty(column.name()).trim().toLowerCase();

        if (embedded && annotatedName.equals("_")) {
            return prefix;
        }

        if (!annotatedName.isEmpty()) {
            return prefix + annotatedName;
        }

        propertyName = Strings.nullToEmpty(propertyName).trim();
        if (propertyName.isEmpty()) {
            return prefix;
        }

        propertyName = SqlUtils.sqlName(propertyName);
        if (embedded && !propertyName.endsWith("_")) {
            propertyName = propertyName + "_";
        }

        return prefix + propertyName;
    }

    static Method[] sortByInheritance(Method[] methods) {
        final Map<Class, List<Method>> map = Maps.newLinkedHashMap();
        for (Method method : methods) {
            List<Method> list = map.get(method.getDeclaringClass());
            if (list == null) {
                list = Lists.newArrayList(method);
                map.put(method.getDeclaringClass(), list);
            } else {
                list.add(method);
            }
        }

        final List<Method> returnList = Lists.newArrayListWithExpectedSize(methods.length);
        for (List<Method> clsMethods : map.values()) {
            returnList.addAll(0, clsMethods);
        }

        return returnList.toArray(new Method[returnList.size()]);
    }

    public Class<? extends AbstractEntity> getEntityClass() {
        return entityClass;
    }

    public boolean isNativeType() {
        return nativeType;
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

            if (Modifier.isPublic(field.getModifiers())) {
                throw new PersistException("Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName() + "] cannot be 'public'.");
            }

            // Ignore no column.
            if (field.getDeclaredAnnotation(NoColumn.class) != null) {
                continue;
            }

            final Column columnAnnotation = field.getDeclaredAnnotation(Column.class);
            // Ensure @Column annotation.
            if (columnAnnotation == null) {
                throw new PersistException("Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName() + "] does not have a @Column annotation.");
            }

            final String getterName = getterName(field.getName(), field.getType());
            final Method getter = methodMap.get(getterName);
            if (getter == null) {
                throw new PersistException("Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName() + "] does not have a getter [" + getterName + "].");
            }
            if (!Modifier.isPublic(getter.getModifiers())) {
                throw new PersistException("Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName() + "] getter [" + getterName + "] must be public.");
            }
            if (Modifier.isStatic(getter.getModifiers())) {
                throw new PersistException("Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName() + "] getter [" + getterName + "] cannot be static.");
            }
            if (getter.getParameterCount() != 0) {
                throw new PersistException("Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName() + "] getter [" + getterName + "] cannot have any parameters.");
            }
            if (getter.getReturnType() != field.getType()) {
                throw new PersistException("Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName() + "] getter [" + getterName + "] have type mismatch [" + field.getType().getCanonicalName() + "] != [" + getter.getReturnType().getCanonicalName() + "].");
            }

            final String setterName = setterName(field.getName());
            final Method setter = methodMap.get(setterName);
            if (setter == null) {
                throw new PersistException("Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName() + "] does not have a setter [" + setterName + "]");
            }
            if (!Modifier.isPublic(setter.getModifiers())) {
                throw new PersistException("Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName() + "] getter [" + setterName + "] must be public.");
            }
            if (Modifier.isStatic(setter.getModifiers())) {
                throw new PersistException("Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName() + "] getter [" + setterName + "] cannot be static.");
            }
            if (setter.getParameterCount() != 1) {
                throw new PersistException("Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName() + "] setter [" + setterName + "] must have exactly 1 parameter.");
            }
            if (setter.getParameterTypes()[0] != field.getType()) {
                throw new PersistException("Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName() + "] setter [" + setterName + "] have type mismatch [" + field.getType().getCanonicalName() + "] != [" + setter.getParameterTypes()[0].getCanonicalName() + "].");
            }
            if (setter.getReturnType() != void.class) {
                throw new PersistException("Entity Class [" + objectClass.getCanonicalName() + "] field [" + field.getName() + "] setter [" + setterName + "] cannot return anything.");
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
            property.columnName = columnName(columnNamePrefix, columnAnnotation, property.name, property.embedded);
            property.prefix = columnNamePrefix;

            if (!property.type.isPrimitive())
                property.fastClass = FastClass.create(property.type);

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
                } else if (property.columnName.toLowerCase().endsWith("_id") || property.columnName.toLowerCase().equals("id")) {
                    property.columnLength = COLUMN_LENGTH_ID;
                } else {
                    property.columnLength = COLUMN_LENGTH_STRING;
                }
            }

            property.dbType = property.toDBType();

            // Handle embedded types.
            // Embedded types are essentially unwrapped and flattened out.
            // This supports nested embedding.
            if (property.embedded) {
                final Property[] children = extractFields(property.type, property, property.columnName);
                if (children == null || children.length == 0) {
                    throw new PersistException("Class [" + objectClass.getCanonicalName() + "] specified an embedded field of type [" + property.type.getCanonicalName() + "] which has no fields specified.");
                }
                properties.addAll(Lists.newArrayList(children));
            } else {
                properties.add(property);
            }
        }

        return properties.toArray(new Property[properties.size()]);
    }

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
        protected Field jooqJournalField;
        protected Object[] enumConstants;
        protected Map<String, Object> enumConstantMap;
        private int columnLength;
        private boolean nullable;

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

        public void set(Object obj, Object value) throws InvocationTargetException, IllegalAccessException {
            if (enumType && value != null && value instanceof String) {
                final Object enumConstant = enumConstantMap.get(value);
                setter.invoke(getParentInstance(obj), new Object[]{enumConstant});
            } else {
                setter.invoke(getParentInstance(obj), new Object[]{value});
            }
        }

        public Object getParentInstance(Object root) throws InvocationTargetException, IllegalAccessException {
            if (parent == null) {
                return root;
            }

            return parent.getInstance(root);
        }

        public Object getOrCreate(Object parent) throws InvocationTargetException, IllegalAccessException {
            Object current = reflectField.get(parent);
            if (current == null) {
                current = fastClass.newInstance();
                reflectField.set(parent, current);
            }
            return current;
        }

        public Object getInstance(Object root) throws InvocationTargetException, IllegalAccessException {
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

            DataType dataType = dbPlatform.fromJdbcType(column.dataType);
            if (dataType == null) {
                return null;
            }

            dataType = dataType.nullable(column.nullable);
            dataType = dataType.length(column.columnSize);

            return dataType;
        }

        public DataType fieldDataType() {
            DataType dataType = dbPlatform.fromJdbcType(dbType);
            if (dataType == null) {
                return null;
            }

            dataType = dataType.nullable(nullable);
            dataType = dataType.length(columnLength);

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

            if (type.isEnum()) {
                return DBTypes.VARCHAR;
            }

            return dbType;
        }
    }
}
