package move.sql;

import org.jooq.Configuration;
import org.jooq.DataType;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.util.h2.H2DataType;

/**
 *
 */
public class H2Platform extends SqlPlatform {
    public H2Platform(Configuration configuration, SqlConfig configEntity) {
        super(configuration, configEntity);
    }

    public SQLDialect dialect() {
        return SQLDialect.H2;
    }

    @Override
    public String ddlCreateIndex(TableMapping mapping, TableMapping.Index index) {
        final String name = index.name;

        if (index.unique) {
            return create().createUniqueIndex(name)
                .on(mapping.getTableName(), index.columnNames).getSQL(ParamType.INLINED);
        }

        return create().createIndex(name).on(mapping.getTableName(), index.columnNames).getSQL(ParamType.INLINED);
    }

    public DataType fromJdbcType(int type) {
        switch (type) {
            case DBTypes.ENUM:
                return H2DataType.VARCHAR.length(128);
            case DBTypes.BIGINT:
                return H2DataType.BIGINT;
            case DBTypes.BOOLEAN:
                return H2DataType.BOOLEAN;
            case DBTypes.BIT:
                return H2DataType.BOOLEAN;
            case DBTypes.TIMESTAMP:
                return H2DataType.DATETIME;
            case DBTypes.TIME:
                return H2DataType.DATETIME;
            case DBTypes.VARBINARY:
                return H2DataType.VARBINARY;
            case DBTypes.BINARY:
                return H2DataType.BINARY;
            case DBTypes.BLOB:
                return H2DataType.BLOB;
            case DBTypes.LONGNVARCHAR:
            case DBTypes.LONGVARCHAR:
            case DBTypes.CLOB:
                return H2DataType.TEXT;
            case DBTypes.DATE:
                return H2DataType.DATETIME;
            case DBTypes.DECIMAL:
                return H2DataType.DECIMAL;
            case DBTypes.DOUBLE:
                return H2DataType.DOUBLE;
            case DBTypes.FLOAT:
                return H2DataType.FLOAT;
            case DBTypes.INTEGER:
                return H2DataType.INTEGER;
            case DBTypes.CHAR:
                return H2DataType.CHAR;
            case DBTypes.NCHAR:
                return H2DataType.CHAR;
            case DBTypes.SMALLINT:
                return H2DataType.SMALLINT;
            case DBTypes.VARCHAR:
                return H2DataType.VARCHAR;
            case DBTypes.NVARCHAR:
                return H2DataType.VARCHAR;
        }
        return null;
    }
}
