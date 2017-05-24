package move.sql;

import org.jooq.DataType;
import org.jooq.SQLDialect;
import org.jooq.impl.*;
import org.jooq.impl.PrecisionDataType;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/**
 *
 */
public class NuoDBDataType {

    // -------------------------------------------------------------------------
    // Default SQL data types and synonyms thereof
    // -------------------------------------------------------------------------

    public static final DataType<String> STRING = new DefaultDataType<>(SQLDialect.MYSQL, SQLDataType.LONGVARCHAR, "string", "string");
    public static final DataType<String> VARCHAR = new DefaultDataType<>(SQLDialect.MYSQL, SQLDataType.VARCHAR, "varchar", "varchar");
    public static final DataType<String> TEXT = new DefaultDataType<>(SQLDialect.MYSQL, SQLDataType.LONGVARCHAR, "text", "text");


    public static final DataType<Short> SMALLINT = new DefaultDataType<>(SQLDialect.MYSQL, SQLDataType.SMALLINT, "smallint", "smallint");
    public static final DataType<Integer> INTEGER = new DefaultDataType<>(SQLDialect.MYSQL, SQLDataType.INTEGER, "integer", "integer");
    public static final DataType<Long> BIGINT = new DefaultDataType<Long>(SQLDialect.MYSQL, SQLDataType.BIGINT, "bigint", "bigint");
    public static final DataType<Double> DOUBLE = new DefaultDataType<>(SQLDialect.MYSQL, SQLDataType.DOUBLE, "double", "double");
    public static final DataType<BigDecimal> DECIMAL = new PrecisionDataType<>(SQLDialect.MYSQL, SQLDataType.DECIMAL, "decimal", "decimal");
    public static final DataType<Double> NUMERIC = new PrecisionDataType<>(SQLDialect.MYSQL, SQLDataType.DOUBLE, "numeric", "numeric");

    public static final DataType<Boolean> BOOLEAN = new DefaultDataType<Boolean>(SQLDialect.MYSQL, SQLDataType.BOOLEAN, "boolean", "boolean");
    public static final DataType<byte[]> VARBINARY = new DefaultDataType<byte[]>(SQLDialect.MYSQL, SQLDataType.VARBINARY, "varbinary", "varbinary");
    public static final DataType<byte[]> BINARY = new DefaultDataType<byte[]>(SQLDialect.MYSQL, SQLDataType.BINARY, "blob", "blob");
    public static final DataType<byte[]> BLOB = new DefaultDataType<byte[]>(SQLDialect.MYSQL, SQLDataType.BLOB, "blob", "blob");

    public static final DataType<String> ENUM = new DefaultDataType<>(SQLDialect.MYSQL, SQLDataType.VARCHAR, "enum", "string");

    public static final DataType<Date> DATE = new DefaultDataType<>(SQLDialect.MYSQL, SQLDataType.DATE, "date", "date");
    public static final DataType<Time> TIME = new DefaultDataType<>(SQLDialect.MYSQL, SQLDataType.TIME, "time", "time");
    public static final DataType<Timestamp> TIMESTAMP = new DefaultDataType<>(SQLDialect.MYSQL, SQLDataType.TIMESTAMP, "timestamp", "timestamp");
}
