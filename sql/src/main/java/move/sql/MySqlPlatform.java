package move.sql;

import com.google.common.base.Strings;
import org.jooq.Configuration;
import org.jooq.DataType;
import org.jooq.impl.SQLDataType;
import org.jooq.util.mysql.MySQLDataType;

/**
 * MySQL platform.
 *
 * @author Clay Molocznik
 */
public class MySqlPlatform extends SqlPlatform {
    public MySqlPlatform(Configuration configuration, SqlConfig configEntity) {
        super(configuration, configEntity);
    }

    /**
     * @return
     */
    @Override
    public String quote() {
        return "`";
    }

    /**
     * @param mapping
     * @return
     */
    @Override
    public String ddlCreateTable(TableMapping mapping) {
        String sql = super.ddlCreateTable(mapping);

        sql = sql.trim();

        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 2);
        }

        sql += " ENGINE = " + getStorageEngine() + " DEFAULT CHARSET=utf8;";
        return sql;
    }

    /**
     * @return
     */
    public String getStorageEngine() {
        final SqlConfig config = getConfigEntity();
        final String engine = config != null ? Strings.nullToEmpty(config.getStorageEngine()).trim() : "";
        return engine.isEmpty() ? "InnoDB" : engine;
    }

    /**
     * @param type
     * @return
     */
    public DataType fromJdbcType(int type, TableMapping.Property property) {
        switch (type) {
            case DBTypes.ENUM:
                return MySQLDataType.VARCHAR.length(128);
            case DBTypes.BIGINT:
                return MySQLDataType.BIGINT;
            case DBTypes.BOOLEAN:
            case DBTypes.BIT:
                return MySQLDataType.BIT;
            case DBTypes.TIMESTAMP:
                return MySQLDataType.DATETIME;
            case DBTypes.TIME:
                return MySQLDataType.TIME;
            case DBTypes.VARBINARY:
                return MySQLDataType.VARBINARY;
            case DBTypes.BINARY:
                return MySQLDataType.BINARY;
            case DBTypes.BLOB:
                return MySQLDataType.BLOB;
            case DBTypes.LONGVARCHAR:
            case DBTypes.LONGNVARCHAR:
            case DBTypes.CLOB:
                return MySQLDataType.TEXT;
            case DBTypes.DATE:
                return MySQLDataType.DATE;
            case DBTypes.DECIMAL:
                if (property.columnAnnotation != null && property.columnAnnotation.precision() > 0)
                    return MySQLDataType.DECIMAL.precision(property.columnAnnotation.precision(), property.columnAnnotation.scale());
                else
                    return MySQLDataType.DECIMAL;
            case DBTypes.DOUBLE:
                if (property.columnAnnotation != null && property.columnAnnotation.precision() > 0)
                    return MySQLDataType.DOUBLE.precision(property.columnAnnotation.precision(), property.columnAnnotation.scale());
                else
                    return MySQLDataType.DOUBLE;
            case DBTypes.FLOAT:
                if (property.columnAnnotation != null && property.columnAnnotation.precision() > 0)
                    return MySQLDataType.FLOAT.precision(property.columnAnnotation.precision(), property.columnAnnotation.scale());
                else
                    return MySQLDataType.FLOAT;
            case DBTypes.INTEGER:
                return MySQLDataType.INTEGER;
            case DBTypes.CHAR:
                return MySQLDataType.CHAR;
            case DBTypes.NCHAR:
                return MySQLDataType.CHAR;
            case DBTypes.SMALLINT:
                return MySQLDataType.SMALLINT;
            case DBTypes.VARCHAR:
                return MySQLDataType.VARCHAR;
            case DBTypes.NVARCHAR:
                return MySQLDataType.VARCHAR;
        }
        return null;
    }
}
