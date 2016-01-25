package io.clickhandler.sql.db;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public class SqlUtils {
    public static final Logger LOG = LoggerFactory.getLogger(SqlUtils.class);

    public static final String[] ALPHABET = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"};

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
                || type == Blob.class || type == Object.class);
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

    /**
     * Sets parameters in the given prepared statement.
     * <p/>
     * Parameters will be set using PreparedStatement set methods related with
     * the Java types of the parameters, according with the following table:
     * <ul>
     * <li> Boolean/boolean: setBoolean
     * <li> Byte/byte: setByte
     * <li> Short/short: setShort
     * <li> Integer/integer: setInt
     * <li> Long/long: setLong
     * <li> Float/float: setFloat
     * <li> Double/double: setDouble
     * <li> Character/char: setString
     * <li> Character[]/char[]: setString
     * <li> Byte[]/byte[]: setBytes
     * <li> String: setString
     * <li> java.math.BigDecimal: setBigDecimal
     * <li> java.io.Reader: setCharacterStream
     * <li> java.io.InputStream: setBinaryStream
     * <li> java.util.Date: setTimestamp
     * <li> java.sql.Date: setDate
     * <li> java.sql.Time: setTime
     * <li> java.sql.Timestamp: setTimestamp
     * <li> java.sql.Clob : setClob
     * <li> java.sql.Blob: setBlob
     * </ul>
     *
     * @param stmt       {@link java.sql.PreparedStatement} to have parameters set into
     * @param parameters varargs or Object[] with parameters values
     * @throws PersistException if a database access error occurs or this
     *                          method is called on a closed PreparedStatement; if a parameter type does
     *                          not have a matching set method (as outlined above)
     * @throws PersistException if an error occurs while reading data from a
     *                          Reader or InputStream parameter
     * @since 1.0
     */
    public static void setParameters(final PreparedStatement stmt, final Object[] parameters) {
        // If no parameters, do nothing.
        if (parameters == null || parameters.length == 0) {
            return;
        }

        ParameterMetaData stmtMetaData = null;

        for (int i = 1; i <= parameters.length; i++) {
            final Object parameter = parameters[i - 1];

            if (parameter == null) {
                // Lazy assignment of stmtMetaData.
                if (stmtMetaData == null) {
                    try {
                        stmtMetaData = stmt.getParameterMetaData();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }

                // Get sql type from prepared statement metadata.
                int sqlType;

                try {
                    sqlType = stmtMetaData.getParameterType(i);
                } catch (SQLException e2) {
                    // Feature not supported, use NULL.
                    sqlType = Types.NULL;
                }

                try {
                    stmt.setNull(i, sqlType);
                } catch (SQLException e) {
                    throw new PersistException("Could not set null into parameter [" + i
                            + "] using java.sql.Types [" + sqlTypeToString(sqlType) + "] " + e.getMessage(), e);
                }

                continue;
            }

            try {
                final Class type = parameter.getClass();

                if (type == Boolean.class || type == boolean.class) {
                    stmt.setBoolean(i, (Boolean) parameter);
                } else if (type == Byte.class || type == byte.class) {
                    stmt.setByte(i, (Byte) parameter);
                } else if (type == Short.class || type == short.class) {
                    stmt.setShort(i, (Short) parameter);
                } else if (type == Integer.class || type == int.class) {
                    stmt.setInt(i, (Integer) parameter);
                } else if (type == Long.class || type == long.class) {
                    stmt.setLong(i, (Long) parameter);
                } else if (type == Float.class || type == float.class) {
                    stmt.setFloat(i, (Float) parameter);
                } else if (type == Double.class || type == double.class) {
                    stmt.setDouble(i, (Double) parameter);
                } else if (type == Character.class || type == char.class) {
                    stmt.setString(i, parameter == null ? null : "" + (Character) parameter);
                } else if (type == char[].class) {
                    // not efficient, will create a new String object
                    stmt.setString(i, parameter == null ? null : new String((char[]) parameter));
                } else if (type == Character[].class) {
                    // not efficient, will duplicate the array and create a new String object
                    final Character[] src = (Character[]) parameter;
                    final char[] dst = new char[src.length];
                    for (int j = 0; j < src.length; j++) { // can't use System.arraycopy here
                        dst[j] = src[j];
                    }
                    stmt.setString(i, new String(dst));
                } else if (type == String.class) {
                    stmt.setString(i, (String) parameter);
                } else if (type == BigDecimal.class) {
                    stmt.setBigDecimal(i, (BigDecimal) parameter);
                } else if (type == byte[].class) {
                    stmt.setBytes(i, (byte[]) parameter);
                } else if (type == Byte[].class) {
                    // not efficient, will duplicate the array
                    final Byte[] src = (Byte[]) parameter;
                    final byte[] dst = new byte[src.length];
                    for (int j = 0; j < src.length; j++) { // can't use System.arraycopy here
                        dst[j] = src[j];
                    }
                    stmt.setBytes(i, dst);
                } else if (parameter instanceof java.io.Reader) {
                    final java.io.Reader reader = (java.io.Reader) parameter;

                    // the jdbc api for setCharacterStream requires the number
                    // of characters to be read so this will end up reading
                    // data twice (here and inside the jdbc driver)
                    // besides, the reader must support reset()
                    int size = 0;
                    try {
                        reader.reset();
                        while (reader.read() != -1) {
                            size++;
                        }
                        reader.reset();
                    } catch (IOException e) {
                        throw new PersistException(e);
                    }
                    stmt.setCharacterStream(i, reader, size);
                } else if (parameter instanceof java.io.InputStream) {
                    final java.io.InputStream inputStream = (java.io.InputStream) parameter;

                    // the jdbc api for setBinaryStream requires the number of
                    // bytes to be read so this will end up reading the stream
                    // twice (here and inside the jdbc driver)
                    // besides, the stream must support reset()
                    int size = 0;
                    try {
                        inputStream.reset();
                        while (inputStream.read() != -1) {
                            size++;
                        }
                        inputStream.reset();
                    } catch (IOException e) {
                        throw new PersistException(e);
                    }
                    stmt.setBinaryStream(i, inputStream, size);
                } else if (parameter instanceof Clob) {
                    stmt.setClob(i, (Clob) parameter);
                } else if (parameter instanceof Blob) {
                    stmt.setBlob(i, (Blob) parameter);
                } else if (type == java.util.Date.class) {
                    final java.util.Date date = (java.util.Date) parameter;
                    stmt.setTimestamp(i, new Timestamp(date.getTime()));
                } else if (type == Date.class) {
                    stmt.setDate(i, (Date) parameter);
                } else if (type == Time.class) {
                    stmt.setTime(i, (Time) parameter);
                } else if (type == Timestamp.class) {
                    stmt.setTimestamp(i, (Timestamp) parameter);
                } else if (type.isEnum()) {
                    stmt.setString(i, parameter.toString());
                } else {
                    // last resort; this should cover all database-specific
                    // object types
                    stmt.setObject(i, parameter);
                }

            } catch (SQLException e) {
                throw new PersistException(e);
            }
        }
    }

    /**
     * Reads a column from the current row in the provided
     * {@link java.sql.ResultSet} and returns an instance of the specified Java
     * {@link Class} containing the values read.
     * <p/>
     * This method is used while converting {@link java.sql.ResultSet} rows to
     * objects. The class type is the field type in the target bean.
     * <p/>
     * Correspondence between class types and ResultSet.get methods is as
     * follows:
     * <ul>
     * <li> Boolean/boolean: getBoolean
     * <li> Byte/byte: getByte
     * <li> Short/short: getShort
     * <li> Integer/int: getInt
     * <li> Long/long: getLong
     * <li> Float/float: getFloat
     * <li> Double/double: getDouble
     * <li> Character/char: getString
     * <li> Character[]/char[]: getString
     * <li> Byte[]/byte[]: setBytes
     * <li> String: setString
     * <li> java.math.BigDecimal: getBigDecimal
     * <li> java.io.Reader: getCharacterStream
     * <li> java.io.InputStream: getBinaryStream
     * <li> java.util.Date: getTimestamp
     * <li> java.sql.Date: getDate
     * <li> java.sql.Time: getTime
     * <li> java.sql.Timestamp: getTimestamp
     * <li> java.sql.Clob: getClob
     * <li> java.sql.Blob: getBlob
     * </ul>
     * <p/>
     * null's will be respected for any non-native types. This means that if a
     * field is of type Integer it will be able to receive a null value from the
     * ResultSet; on the other hand, if a field is of type int it will receive 0
     * for a null value from the {@link java.sql.ResultSet}.
     *
     * @param resultSet {@link java.sql.ResultSet} (positioned in the row to be
     *                  processed)
     * @param column    column index in the result set (starting with 1)
     * @param type      {@link Class} of the object to be returned
     * @since 1.0
     */
    public static Object getValue(final ResultSet resultSet, final int column, final Class type) {
        Object value = null;

        try {
            if (type == boolean.class) {
                value = resultSet.getBoolean(column);
            } else if (type == Boolean.class) {
                value = resultSet.getObject(column) == null ? null : resultSet.getBoolean(column);
            } else if (type == byte.class) {
                value = resultSet.getByte(column);
            } else if (type == Byte.class) {
                value = resultSet.getObject(column) == null ? null : resultSet.getByte(column);
            } else if (type == short.class) {
                value = resultSet.getShort(column);
            } else if (type == Short.class) {
                value = resultSet.getObject(column) == null ? null : resultSet.getShort(column);
            } else if (type == int.class) {
                value = resultSet.getInt(column);
            } else if (type == Integer.class) {
                value = resultSet.getObject(column) == null ? null : resultSet.getInt(column);
            } else if (type == long.class) {
                value = resultSet.getLong(column);
            } else if (type == Long.class) {
                value = resultSet.getObject(column) == null ? null : resultSet.getLong(column);
            } else if (type == float.class) {
                value = resultSet.getFloat(column);
            } else if (type == Float.class) {
                value = resultSet.getObject(column) == null ? null : resultSet.getFloat(column);
            } else if (type == double.class) {
                value = resultSet.getDouble(column);
            } else if (type == Double.class) {
                value = resultSet.getObject(column) == null ? null : resultSet.getDouble(column);
            } else if (type == BigDecimal.class) {
                value = resultSet.getObject(column) == null ? null : resultSet.getBigDecimal(column);
            } else if (type == String.class) {
                value = resultSet.getString(column);
            } else if (type == Character.class || type == char.class) {
                final String str = resultSet.getString(column);
                if (str != null && str.length() > 1) {
                    throw new PersistException("Column [" + column + "] returned a string with length ["
                            + str.length() + "] but field type [" + type.getSimpleName()
                            + "] can only accept 1 character");
                }
                value = (str == null || str.length() == 0) ? null : str.charAt(0);
            } else if (type == byte[].class || type == Byte[].class) {
                value = resultSet.getBytes(column);
            } else if (type == char[].class || type == Character[].class) {
                final String str = resultSet.getString(column);
                value = (str == null) ? null : str.toCharArray();
            } else if (type == java.util.Date.class) {
                final Timestamp timestamp = resultSet.getTimestamp(column);
                value = (timestamp == null) ? null : new java.util.Date(timestamp.getTime());
            } else if (type == Date.class) {
                value = resultSet.getDate(column);
            } else if (type == Time.class) {
                value = resultSet.getTime(column);
            } else if (type == Timestamp.class) {
                value = resultSet.getTimestamp(column);
            } else if (type == java.io.InputStream.class) {
                value = resultSet.getBinaryStream(column);
            } else if (type == java.io.Reader.class) {
                value = resultSet.getCharacterStream(column);
            } else if (type == Clob.class) {
                value = resultSet.getClob(column);
            } else if (type == Blob.class) {
                value = resultSet.getBlob(column);
            } else if (type.isEnum()) {
                final String strValue = Strings.nullToEmpty(resultSet.getString(column));
                try {
                    value = strValue.isEmpty() ? null : Enum.valueOf(type, resultSet.getString(column));
                } catch (Exception e) {
                    LOG.warn("Invalid Enum Value [" + strValue + "] for Enum type [" + type.getCanonicalName() + "]");
                    value = null;
                }
            } else {
                // this will work for database-specific types
                value = resultSet.getObject(column);
            }
        } catch (SQLException e) {
            throw new PersistException(e);
        }

        return value;
    }

    /**
     * Reads a column from the current row in the provided
     * {@link java.sql.ResultSet} and return a value correspondent to the SQL
     * type provided (as defined in {@link java.sql.Types java.sql.Types}).
     * <p/>
     * This method is used while converting results sets to maps. The SQL type
     * comes from the {@link java.sql.ResultSetMetaData ResultSetMetaData} for a
     * given column.
     * <p/>
     * Correspondence between {@link java.sql.Types java.sql.Types} and
     * ResultSet.get methods is as follows:
     * <ul>
     * <li> ARRAY: getArray
     * <li> BIGINT: getLong
     * <li> BIT: getBoolean
     * <li> BLOB: getBytes
     * <li> BOOLEAN: getBoolean
     * <li> CHAR: getString
     * <li> CLOB: getString
     * <li> DATALINK: getBinaryStream
     * <li> DATE: getDate
     * <li> DECIMAL: getBigDecimal
     * <li> DOUBLE: getDouble
     * <li> FLOAT: getFloat
     * <li> INTEGER: getInt
     * <li> JAVA_OBJECT: getObject
     * <li> LONGVARBINARY: getBytes
     * <li> LONGVARCHAR: getString
     * <li> NULL: getNull
     * <li> NCHAR: getString
     * <li> NUMERIC: getBigDecimal
     * <li> OTHER: getObject
     * <li> REAL: getDouble
     * <li> REF: getRef
     * <li> SMALLINT: getInt
     * <li> TIME: getTime
     * <li> TIMESTAMP: getTimestamp
     * <li> TINYINT: getInt
     * <li> VARBINARY: getBytes
     * <li> VARCHAR: getString
     * <li> [Oracle specific] 100: getFloat
     * <li> [Oracle specific] 101: getDouble
     * </ul>
     * <p/>
     * null's are respected for all types. This means that if a column is of
     * type LONG and its value comes from the database as null, this method will
     * return null for it.
     *
     * @param resultSet {@link java.sql.ResultSet} (positioned in the row to be
     *                  processed)
     * @param column    Column index in the result set (starting with 1)
     * @param type      type of the column (as defined in
     *                  {@link java.sql.Types java.sql.Types})
     * @since 1.0
     */
    public static Object getValue(final ResultSet resultSet, final int column, final int type) {
        Object value = null;

        try {
            switch (type) {
                case Types.ARRAY:
                    value = resultSet.getArray(column);
                    break;
                case Types.BIGINT:
                    value = resultSet.getObject(column) == null ? null : resultSet.getLong(column);
                    break;
                case Types.BINARY:
                    value = resultSet.getBytes(column);
                    break;
                case Types.BIT:
                    value = resultSet.getObject(column) == null ? null : resultSet.getBoolean(column);
                    break;
                case Types.BLOB:
                    value = resultSet.getBytes(column);
                    break;
                case Types.BOOLEAN:
                    value = resultSet.getObject(column) == null ? null : resultSet.getBoolean(column);
                    break;
                case Types.CHAR:
                    value = resultSet.getString(column);
                    break;
                case Types.CLOB:
                    value = resultSet.getString(column);
                    break;
                case Types.DATALINK:
                    value = resultSet.getBinaryStream(column);
                    break;
                case Types.DATE:
                    value = resultSet.getDate(column);
                    break;
                case Types.DECIMAL:
                    value = resultSet.getBigDecimal(column);
                    break;
                case Types.DOUBLE:
                    value = resultSet.getObject(column) == null ? null : resultSet.getDouble(column);
                    break;
                case Types.FLOAT:
                    value = resultSet.getObject(column) == null ? null : resultSet.getFloat(column);
                    break;
                case Types.INTEGER:
                    value = resultSet.getObject(column) == null ? null : resultSet.getInt(column);
                    break;
                case Types.JAVA_OBJECT:
                    value = resultSet.getObject(column);
                    break;
                case Types.LONGVARBINARY:
                    value = resultSet.getBytes(column);
                    break;
                case Types.LONGVARCHAR:
                    value = resultSet.getString(column);
                    break;
                case Types.NULL:
                    value = null;
                    break;
                case Types.NUMERIC:
                    value = resultSet.getBigDecimal(column);
                    break;
                case Types.OTHER:
                    value = resultSet.getObject(column);
                    break;
                case Types.REAL:
                    value = resultSet.getObject(column) == null ? null : resultSet.getDouble(column);
                    break;
                case Types.REF:
                    value = resultSet.getRef(column);
                    break;
                case Types.SMALLINT:
                    value = resultSet.getObject(column) == null ? null : resultSet.getInt(column);
                    break;
                case Types.TIME:
                    value = resultSet.getTime(column);
                    break;
                case Types.TIMESTAMP:
                    value = resultSet.getTimestamp(column);
                    break;
                case Types.TINYINT:
                    value = resultSet.getObject(column) == null ? null : resultSet.getInt(column);
                    break;
                case Types.VARBINARY:
                    value = resultSet.getBytes(column);
                    break;
                case Types.VARCHAR:
                    value = resultSet.getString(column);
                    break;

                // Oracle.
                case 100:
                    value = resultSet.getObject(column) == null ? null : resultSet.getFloat(column);
                    break;
                case 101:
                    value = resultSet.getObject(column) == null ? null : resultSet.getDouble(column);
                    break;

                default:
                    throw new PersistException("Could not get value for result set using type ["
                            + sqlTypeToString(type) + "] on column [" + column + "]");
            }
        } catch (SQLException e) {
            throw new PersistException(e);
        }

        return value;
    }

    /**
     * Converts types expected in prepared statement parameters to a suitable
     * string to be added to log output.
     */
    public static String sqlTypeToString(final int type) {
        final String ret;

        if (type == Types.ARRAY) {
            ret = "ARRAY";
        } else if (type == Types.BIGINT) {
            ret = "BIGINT";
        } else if (type == Types.BINARY) {
            ret = "BINARY";
        } else if (type == Types.BIT) {
            ret = "BIT";
        } else if (type == Types.BLOB) {
            ret = "BLOB";
        } else if (type == Types.BOOLEAN) {
            ret = "BOOLEAN";
        } else if (type == Types.CHAR) {
            ret = "CHAR";
        } else if (type == Types.CLOB) {
            ret = "CLOB";
        } else if (type == Types.DATALINK) {
            ret = "DATALINK";
        } else if (type == Types.DATE) {
            ret = "DATE";
        } else if (type == Types.DECIMAL) {
            ret = "DECIMAL";
        } else if (type == Types.DISTINCT) {
            ret = "DISTINCT";
        } else if (type == Types.DOUBLE) {
            ret = "DOUBLE";
        } else if (type == Types.FLOAT) {
            ret = "FLOAT";
        } else if (type == Types.INTEGER) {
            ret = "INTEGER";
        } else if (type == Types.JAVA_OBJECT) {
            ret = "JAVA_OBJECT";
        } else if (type == Types.LONGVARBINARY) {
            ret = "LONGVARBINARY";
        } else if (type == Types.LONGVARCHAR) {
            ret = "LONGVARCHAR";
        } else if (type == Types.NULL) {
            ret = "NULL";
        } else if (type == Types.NUMERIC) {
            ret = "NUMERIC";
        } else if (type == Types.OTHER) {
            ret = "OTHER";
        } else if (type == Types.REAL) {
            ret = "REAL";
        } else if (type == Types.REF) {
            ret = "REF";
        } else if (type == Types.SMALLINT) {
            ret = "SMALLINT";
        } else if (type == Types.STRUCT) {
            ret = "STRUCT";
        } else if (type == Types.TIME) {
            ret = "TIME";
        } else if (type == Types.TIMESTAMP) {
            ret = "TIMESTAMP";
        } else if (type == Types.TINYINT) {
            ret = "TINYINT";
        } else if (type == Types.VARBINARY) {
            ret = "VARBINARY";
        } else if (type == Types.VARCHAR) {
            ret = "VARCHAR";
        } else {
            ret = "" + type;
        }

        return ret;
    }

    /**
     * Reads a row from the provided {@link java.sql.ResultSet} and converts it
     * to a map having the column names as keys and results as values.
     * <p/>
     * See {@link SqlUtils#getValue(java.sql.ResultSet, int, int)} for details on
     * the mappings between ResultSet.get methods and
     * {@link java.sql.Types java.sql.Types} types
     *
     * @param resultSet {@link java.sql.ResultSet} (positioned in the row to be
     *                  processed)
     * @since 1.0
     */
    public static Map<String, Object> loadMap(final ResultSet resultSet) throws SQLException {
        final Map<String, Object> ret = new LinkedHashMap<String, Object>();
        final ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

        for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
            final String columnName = resultSetMetaData.getColumnName(i);
            if (columnName.equalsIgnoreCase("RN__")) {
                continue;
            }

            final int type = resultSetMetaData.getColumnType(i);
            final Object value = getValue(resultSet, i, type);
            ret.put(columnName, value);
        }

        return ret;
    }

    /**
     * Returns a list of values for the fields in the provided object that match
     * the provided list of columns according with the object mapping.
     *
     * @param object  source object to obtain parameter values
     * @param columns name of the database columns to get parameters for
     * @param mapping mapping for the object class
     * @since 1.0
     */
    public static Object[] getParametersFromObject(final Object object,
                                                   final String[] columns,
                                                   final Mapping mapping) {
        Object[] parameters = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            final String columnName = columns[i];
            final Mapping.Property property = mapping.getProperty(columnName);

            if (property == null) {
                throw new PersistException("Could not access getter for column [" + columnName + "] because column is not mapped.");
            }

            Object value;
            try {
                value = property.get(object);
            } catch (Exception e) {
                throw new PersistException("Could not access getter for column [" + columnName + "].", e);
            }
            parameters[i] = value;
        }

        return parameters;
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
}
