package io.clickhandler.sql;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.jooq.Configuration;
import org.jooq.CreateTableAsStep;
import org.jooq.CreateTableColumnStep;
import org.jooq.DataType;
import org.jooq.impl.DSL;
import org.jooq.util.mysql.MySQLDataType;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * MySQL platform.
 *
 * @author Clay Molocznik
 */
public class MemSqlPlatform extends SqlPlatform {
    public MemSqlPlatform(Configuration configuration, SqlConfig configEntity) {
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
        final CreateTableAsStep step = create().createTable(mapping.getTableName());
        CreateTableColumnStep createColumnStep = null;
        for (TableMapping.Property property : mapping.getProperties()) {
            createColumnStep = step.column(property.getColumnName(), property.fieldDataType());
        }

        if (!mapping.tableAnnotation.columnStore()) {
            createColumnStep.constraints(DSL.constraint("pk_" + mapping.getTableName())
                .primaryKey(columnNames(mapping.getPrimaryKeyProperties())));

            final List<TableMapping.Index> indexes = mapping.getIndexes();
            if (indexes != null) {
                for (TableMapping.Index index : indexes) {
                    if (index.unique) {
                        final LinkedHashSet<String> columns = new LinkedHashSet<>();
                        columns.addAll(Lists.newArrayList(columnNames(mapping.getPrimaryKeyProperties())));
                        columns.addAll(Lists.newArrayList(index.columnNames));

                        createColumnStep.constraint(DSL.constraint(index.name)
                            .unique(columns.toArray(new String[columns.size()])));
                    }
                }
            }
        }

        String sql = createColumnStep.getSQL();

        // Handle reference tables.
        if (mapping.tableAnnotation.reference()) {
            if (sql.startsWith("create table") || sql.startsWith("CREATE TABLE")) {
                sql = sql.substring(12);
                sql = "CREATE REFERENCE TABLE" + sql;
            }
        }

        sql = sql.trim();

        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }

        if (sql.endsWith(")")) {
            sql = sql.substring(0, sql.length() - 1);
        }

        if (mapping.tableAnnotation.columnStore()) {
            final List<TableMapping.Property> primaryKey = mapping.getPrimaryKeyProperties();

            sql += ", KEY (" + Joiner.on(",").join(columnNames(primaryKey)) + ") USING CLUSTERED COLUMNSTORE";
        } else {
            final List<TableMapping.Property> primaryKey = mapping.getPrimaryKeyProperties();

            // Add index on "id" column if it isn't the primary key.
            if (primaryKey.size() == 1 && !primaryKey.get(0).columnName.equals(AbstractEntity.ID)) {
                sql += ", KEY ix_" + mapping.getTableName() + "_id (`id`)";
            }
        }

        if (!mapping.tableAnnotation.reference()) {
            final List<TableMapping.Property> shardKey = mapping.getShardKeyProperties();
            if (shardKey != null && !shardKey.isEmpty()) {
                sql = sql + ", SHARD KEY (" +
                    Joiner.on(",").join(columnNames(shardKey)) +
                    ")";
            }
        }

        sql += ")";

        return sql;
    }

    @Override
    public String ddlCreateIndex(TableMapping mapping, TableMapping.Index index) {
        final String name = index.name;

        if (index.unique) {
            return null;
            // Prepend Primary Key.
//            final LinkedHashSet<String> columns = new LinkedHashSet<>();
//            columns.addAll(Lists.newArrayList(columnNames(mapping.getPrimaryKeyProperties())));
//            columns.addAll(Lists.newArrayList(index.columnNames));
//            return create()
//                .alterTable(mapping.getTableName())
//                .add(DSL.constraint(name).unique(columns.toArray(new String[columns.size()]))).getSQL();
        }

        return create()
            .createIndex(name)
            .on(mapping.getTableName(), index.columnNames).getSQL();
    }

    /**
     * @param type
     * @return
     */
    public DataType fromJdbcType(int type) {
        switch (type) {
            case DBTypes.BIGINT:
                return MySQLDataType.BIGINT;
            case DBTypes.BOOLEAN:
            case DBTypes.BIT:
                return MySQLDataType.BOOLEAN;
            case DBTypes.TIMESTAMP:
                return MySQLDataType.TIMESTAMP;
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
                return MySQLDataType.DECIMAL;
            case DBTypes.DOUBLE:
                return MySQLDataType.DOUBLE;
            case DBTypes.FLOAT:
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
