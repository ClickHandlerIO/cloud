package io.clickhandler.sql.db;

import io.clickhandler.sql.annotations.DBTypes;
import org.jooq.*;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * SqlDatabase app specific settings.
 *
 * @author Clay Molocznik
 */
public class SqlPlatform {
    /**
     * The Constant logger.
     */
    public static final Logger LOG = LoggerFactory.getLogger(SqlPlatform.class);
    private final Configuration configuration;
    private final SqlConfig configEntity;

    public SqlPlatform(Configuration configuration, SqlConfig configEntity) {
        this.configuration = configuration;
        this.configEntity = configEntity;
    }

    /**
     * @param properties
     * @return
     */
    public static String[] columnNames(List<Mapping.Property> properties) {
        final List<String> names = new ArrayList<>(properties.size());
        for (Mapping.Property property : properties) {
            names.add(property.getColumnName());
        }
        return names.toArray(new String[names.size()]);
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public SqlConfig getConfigEntity() {
        return configEntity;
    }

    public SQLDialect dialect() {
        return SQLDialect.MYSQL;
    }

    public Settings settings() {
        return configuration.settings();
    }

    public DSLContext create() {
        return DSL.using(configuration);
    }

    public String quote() {
        return "\"";
    }

    public String quote(String value) {
        switch (configuration.settings().getRenderNameStyle()) {
            case QUOTED:
                return quote() + value + quote();
            case AS_IS:
                return value;
            case LOWER:
                return quote() + value.toLowerCase() + quote();
            case UPPER:
                return quote() + value.toUpperCase() + quote();
        }

        return quote() + value + quote();
    }

    public String cleanDefaultValue(Object defaultValue, String quote) {
        if (defaultValue == null) {
            return "NULL";
        }

        if (defaultValue instanceof String) {
            String s = (String) defaultValue;
            if (!s.startsWith("\"")) {
                s = quote + s;
            }

            if (!s.endsWith("\"")) {
                s = s + quote;
            }

            defaultValue = s;
        }
        return defaultValue.toString();
    }

    /**
     * @param dbType
     * @return
     */
    public boolean isLengthBased(int dbType) {
        switch (dbType) {
            case DBTypes.CHAR:
            case DBTypes.NCHAR:
            case DBTypes.VARCHAR:
            case DBTypes.NVARCHAR:
            case DBTypes.VARBINARY:
                return true;
        }

        return false;
    }

    /**
     * @param mapping
     * @param journal
     * @return
     */
    public String ddlDropTable(TableMapping mapping, boolean journal) {
        return create().dropTable(mapping.getTableName(journal)).getSQL();
    }

    /**
     * @param mapping
     * @param journal
     * @return
     */
    public String ddlCreateTable(TableMapping mapping, boolean journal) {
        final CreateTableAsStep step = create().createTable(mapping.getTableName(journal));
        CreateTableColumnStep createColumnStep = null;
        for (Mapping.Property property : mapping.getProperties()) {
            createColumnStep = step.column(property.getColumnName(), property.fieldDataType());
        }
        return createColumnStep.getSQL();
    }

    /**
     * @param mapping
     * @param journal
     * @return
     */
    public String ddlPrimaryKey(TableMapping mapping, boolean journal) {
        return create()
            .alterTable(mapping.getTableName(journal))
            .add(DSL.constraint("pk_" + mapping.getTableName(journal))
                .primaryKey(columnNames(mapping.getPrimaryKeyProperties(journal)))).getSQL();
    }

    /**
     * @param mapping
     * @param column
     * @param journal
     * @return
     */
    public String ddlDropColumn(TableMapping mapping, SqlSchema.DbColumn column, boolean journal) {
        return create()
            .alterTable(mapping.getTableName(journal))
            .dropColumn(column.name).getSQL();
    }

    /**
     * @param mapping
     * @param property
     * @param journal
     * @return
     */
    public String ddlAddColumn(TableMapping mapping, Mapping.Property property, boolean journal) {
        return create()
            .alterTable(mapping.getTableName(journal))
            .addColumn(property.getColumnName(), property.fieldDataType()).getSQL();
    }

    /**
     * @param mapping
     * @param property
     * @param journal
     * @return
     */
    public String ddlModifyColumn(TableMapping mapping, Mapping.Property property, boolean journal) {
        return create()
            .alterTable(mapping.getTableName(journal))
            .alterColumn(property.getColumnName()).set(property.fieldDataType()).getSQL();
    }

    /**
     * @param mapping
     * @param index
     * @return
     */
    public String ddlCreateIndex(TableMapping mapping, TableMapping.Index index) {
        final String name = index.name;

        if (index.unique) {
            return create()
                .alterTable(mapping.getTableName(index.journal))
                .add(DSL.constraint(name).unique(index.columnNames)).getSQL();
        }

        return create()
            .createIndex(name)
            .on(mapping.getTableName(index.journal), index.columnNames).getSQL();
    }

    /**
     * @param index
     * @return
     */
    public String ddlDropIndex(SqlSchema.DbIndex index) {
        if (index.unique) {
            return create()
                .alterTable(index.tableName)
                .dropConstraint(index.name).getSQL();
        }
        return create().dropIndex(index.name).getSQL();
    }

    /**
     * @param type
     * @return
     */
    public DataType fromJdbcType(int type) {
        switch (type) {
            case DBTypes.BIGINT:
                return SQLDataType.BIGINT;
            case DBTypes.BOOLEAN:
                return SQLDataType.BOOLEAN;
            case DBTypes.BIT:
                return SQLDataType.BOOLEAN;
            case DBTypes.TIMESTAMP:
                return SQLDataType.TIMESTAMP;
            case DBTypes.TIME:
                return SQLDataType.TIME;
            case DBTypes.VARBINARY:
                return SQLDataType.VARBINARY;
            case DBTypes.BINARY:
                return SQLDataType.BINARY;
            case DBTypes.BLOB:
                return SQLDataType.BLOB;
            case DBTypes.CLOB:
                return SQLDataType.CLOB;
            case DBTypes.DATE:
                return SQLDataType.DATE;
            case DBTypes.DECIMAL:
                return SQLDataType.DECIMAL;
            case DBTypes.DOUBLE:
                return SQLDataType.DOUBLE;
            case DBTypes.FLOAT:
                return SQLDataType.FLOAT;
            case DBTypes.INTEGER:
                return SQLDataType.INTEGER;
            case DBTypes.CHAR:
                return SQLDataType.CHAR;
            case DBTypes.NCHAR:
                return SQLDataType.NVARCHAR;
            case DBTypes.SMALLINT:
                return SQLDataType.SMALLINT;
            case DBTypes.VARCHAR:
                return SQLDataType.VARCHAR;
            case DBTypes.NVARCHAR:
                return SQLDataType.NVARCHAR;
        }
        return null;
    }
}