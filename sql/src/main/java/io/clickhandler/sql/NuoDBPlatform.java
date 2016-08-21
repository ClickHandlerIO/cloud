package io.clickhandler.sql;

import org.jooq.Configuration;
import org.jooq.DataType;

/**
 * NuoDB platform.
 *
 * @author Clay Molocznik
 */
public class NuoDBPlatform extends SqlPlatform {
    public NuoDBPlatform(Configuration configuration, SqlConfig configEntity) {
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

        return sql;

//        if (sql.endsWith(";")) {
//            sql = sql.substring(0, sql.length() - 2);
//        }
//
//        return sql + ");";
    }

    /**
     * @param type
     * @return
     */
    public DataType fromJdbcType(int type) {
        switch (type) {
            case DBTypes.ENUM:
                return NuoDBDataType.ENUM;
            case DBTypes.NUMERIC:
                return NuoDBDataType.NUMERIC;
            case DBTypes.BIGINT:
                return NuoDBDataType.BIGINT;
            case DBTypes.BOOLEAN:
            case DBTypes.BIT:
                return NuoDBDataType.BOOLEAN;
            case DBTypes.TIMESTAMP:
                return NuoDBDataType.TIMESTAMP;
            case DBTypes.TIME:
                return NuoDBDataType.TIME;
            case DBTypes.VARBINARY:
                return NuoDBDataType.VARBINARY;
            case DBTypes.BINARY:
                return NuoDBDataType.BINARY;
            case DBTypes.BLOB:
                return NuoDBDataType.BLOB;
            case DBTypes.LONGVARCHAR:
            case DBTypes.LONGNVARCHAR:
            case DBTypes.CLOB:
                return NuoDBDataType.STRING;
            case DBTypes.DATE:
                return NuoDBDataType.TIMESTAMP;
            case DBTypes.DECIMAL:
                return NuoDBDataType.DECIMAL;
            case DBTypes.DOUBLE:
                return NuoDBDataType.NUMERIC;
            case DBTypes.FLOAT:
                return NuoDBDataType.NUMERIC;
            case DBTypes.INTEGER:
                return NuoDBDataType.INTEGER;
            case DBTypes.CHAR:
                return NuoDBDataType.VARCHAR;
            case DBTypes.NCHAR:
                return NuoDBDataType.VARCHAR;
            case DBTypes.SMALLINT:
                return NuoDBDataType.SMALLINT;
            case DBTypes.VARCHAR:
                return NuoDBDataType.STRING;
            case DBTypes.NVARCHAR:
                return NuoDBDataType.STRING;
        }
        return null;
    }
}
