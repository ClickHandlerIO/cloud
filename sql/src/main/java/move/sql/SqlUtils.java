package move.sql;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jooq.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class SqlUtils {
    public static final Logger LOG = LoggerFactory.getLogger(SqlUtils.class);

    public static final String[] ALPHABET = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};

    public static boolean successful(int affectedRows) {
        return affectedRows > 0;
    }

    public static boolean successful(SqlResult<Integer> result) {
        if (result == null || result.isRollback())
            return false;

        return result.get() != null && result.get() > 0;
    }

    public static boolean successfulBatch(SqlResult<int[]> result) {
        if (result == null || result.isRollback())
            return false;

        return successful(result.get());
    }

    public static boolean successful(int[] affectedRows) {
        if (affectedRows == null || affectedRows.length == 1)
            return false;

        for (int i = 0; i < affectedRows.length; i++)
            if (affectedRows[i] < 1)
                return false;

        return true;
    }

    /**
     *
     * @param name
     * @return
     */
    public static String sqlName(String name) {
        final StringBuilder sb = new StringBuilder();
        final char[] chars = name.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            final char c = chars[i];
            if (i == chars.length - 1) {
                sb.append(Character.toLowerCase(c));
            } else if (Character.isUpperCase(c) && sb.length() > 0) {
                String part = String.valueOf(Character.toLowerCase(c));

                for (i = i + 1; i < chars.length; i++) {
                    final char lookahead = chars[i];
                    if (!Character.isUpperCase(lookahead)) {
                        if (part.length() == 2) {
                            sb.append("_").append(part).append("_").append(Character.toLowerCase(lookahead));
                        } else if (part.length() > 2) {
                            sb.append("_");
                            sb.append(part.substring(0, part.length() - 1));
                            sb.append("_");
                            sb.append(part.substring(part.length() - 1));
                            sb.append(Character.toLowerCase(lookahead));
                        } else {
                            sb.append("_").append(part).append(Character.toLowerCase(lookahead));
                        }
                        part = null;
                        break;
                    } else {
                        part += String.valueOf(Character.toLowerCase(lookahead));
                    }
                }

                if (part != null) {
                    sb.append(part);
                }
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    /**
     * Returns true if the provided class is a type supported natively (as
     * opposed to a bean).
     *
     * @param type {@link Class} type to be tested
     * @since 1.0
     */
    public static boolean isNativeType(final Class type) {
        // To return an arbitrary object use Object.class.
        return (type == boolean.class || type == Boolean.class || type == byte.class || type == Byte.class
                || type == short.class || type == Short.class || type == int.class || type == Integer.class
                || type == long.class || type == Long.class || type == float.class || type == Float.class
                || type == double.class || type == Double.class || type == char.class || type == Character.class
                || type == byte[].class || type == Byte[].class || type == char[].class || type == Character[].class
                || type == String.class || type == BigDecimal.class || type == java.util.Date.class
                || type == java.sql.Date.class || type == Time.class || type == Timestamp.class
                || type == java.io.InputStream.class || type == java.io.Reader.class || type == Clob.class
                || type == Blob.class || type == Object.class || type == java.time.LocalDateTime.class
                || type == ZonedDateTime.class || type == LocalDate.class || type == LocalTime.class);
    }

    /**
     * @param value
     * @param type
     * @param nullable
     * @return
     */
    public static Object parseValue(String value, Class type, boolean nullable) {
        if (type == String.class) {
            return value;
        }

        if (type == Long.class || type == long.class) {
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                return nullable ? null : (long) 0L;
            }
        }

        if (type == Integer.class || type == int.class) {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                return nullable ? null : (int) 0;
            }
        }

        if (type == Boolean.class || type == boolean.class) {
            if (value == null) {
                return nullable ? null : Boolean.FALSE;
            }
            value = value.toLowerCase().trim();
            return value.equals("1") || value.equals("true") || value.equals("yes") || value.equals("t") || value.equals("y");
        }

        if (type == Short.class || type == short.class) {
            try {
                return Short.parseShort(value);
            } catch (Exception e) {
                return nullable ? null : (short) 0;
            }
        }

        if (type == Float.class || type == float.class) {
            try {
                return Float.parseFloat(value);
            } catch (Exception e) {
                return nullable ? null : (float) 0;
            }
        }

        if (type == Double.class || type == double.class) {
            try {
                return Double.parseDouble(value);
            } catch (Exception e) {
                return nullable ? null : (double) 0.0;
            }
        }

        if (type == Character.class || type == char.class) {
            if (value == null) {
                return null;
            }

            if (value.length() > 0) {
                return value.charAt(0);
            } else {
                return null;
            }
        }

        if (type == Byte.class || type == byte.class) {
            try {
                return Byte.parseByte(value);
            } catch (Exception e) {
                return nullable ? null : (byte) 0;
            }
        }

        if (type == java.util.Date.class) {
            try {
                long v = Long.parseLong(value);
                return new java.util.Date(v);
            } catch (Exception e) {
                return null;
            }
        }

        if (type == java.sql.Date.class) {
            try {
                long v = Long.parseLong(value);
                return new java.sql.Date(v);
            } catch (Exception e) {
                return null;
            }
        }

        if (type == Object.class) {
            return value;
        }

        return null;
    }

    public static String minimizedAlias(int index) {
        int count = ALPHABET.length / index;
        int mod = ALPHABET.length % index;
        return ALPHABET[index];
    }

    /**
     * @param sb
     * @param size
     */
    public static void buildInPart(final StringBuilder sb, final int size) {
        if (size < 1) {
            return;
        }

        if (size == 1) {
            sb.append("?");
            return;
        }

        sb.append("?");
        for (int i = 1; i < size; i++) {
            sb.append(",?");
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

        propertyName = sqlName(propertyName);
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

    public static String tableName(Class entityClass, String name) {
        name = Strings.nullToEmpty(name).trim().toLowerCase();

        // Implicitly create TABLE NAME using this convention (SomeSpecialEntity = some_special)
        if (name.isEmpty()) {
            String entityClassName = entityClass.getSimpleName();
            if (entityClassName.endsWith(TableMapping.ENTITY_SUFFIX)) {
                entityClassName = entityClassName.substring(0, entityClassName.length() - TableMapping.ENTITY_SUFFIX.length());
            }
            name = sqlName(entityClassName);
        }

        return name;
    }

    public static List<String> failedQueries(List<? extends Query> queries, int[] results) {
        final List<String> sql = new ArrayList<>();
        for (int i = 0; i < queries.size(); i++) {
            final int result = i < results.length ? results[i] : 0;
            if (result != 1) {
                sql.add(queries.get(i).getSQL());
            }
        }
        return sql;
    }
}
