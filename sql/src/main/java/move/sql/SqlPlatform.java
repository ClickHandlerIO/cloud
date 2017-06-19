package move.sql;

import java.util.ArrayList;
import java.util.List;
import org.jooq.Configuration;
import org.jooq.CreateTableAsStep;
import org.jooq.CreateTableColumnStep;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  public static String[] columnNames(List<TableMapping.Property> properties) {
    final List<String> names = new ArrayList<>(properties.size());
    for (TableMapping.Property property : properties) {
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
   * @return
   */
  public String ddlDropTable(TableMapping mapping) {
    return create().dropTable(mapping.getTableName()).getSQL(ParamType.INLINED);
  }

  /**
   * @param mapping
   * @return
   */
  public String ddlCreateTable(TableMapping mapping) {
    final CreateTableAsStep step = create().createTable(mapping.getTableName());
    CreateTableColumnStep createColumnStep = null;
    for (TableMapping.Property property : mapping.getProperties()) {
      createColumnStep = step.column(property.getColumnName(), property.fieldDataType());
    }
    createColumnStep.constraints(DSL.constraint("pk_" + mapping.getTableName())
        .primaryKey(columnNames(mapping.getPrimaryKeyProperties())));
    return createColumnStep.getSQL(ParamType.INLINED);
  }

  /**
   * @param mapping
   * @return
   */
  public String ddlPrimaryKey(TableMapping mapping) {
    return null;
//        return create()
//            .alterTable(mapping.getTableName())
//            .add(DSL.constraint("pk_" + mapping.getTableName())
//                .primaryKey(columnNames(mapping.getPrimaryKeyProperties()))).getSQL();
  }

  /**
   * @param mapping
   * @param column
   * @return
   */
  public String ddlDropColumn(TableMapping mapping, SqlSchema.DbColumn column) {
    return create()
        .alterTable(mapping.getTableName())
        .dropColumn(column.name)
        .getSQL(ParamType.INLINED);
  }

  /**
   * @param mapping
   * @param property
   * @return
   */
  public String ddlAddColumn(TableMapping mapping, TableMapping.Property property) {
    return create()
        .alterTable(mapping.getTableName())
        .addColumn(property.getColumnName(), property.fieldDataType())
        .getSQL(ParamType.INLINED);
  }

  /**
   * @param mapping
   * @param property
   * @return
   */
  public String ddlModifyColumn(TableMapping mapping, TableMapping.Property property) {
    return create()
        .alterTable(mapping.getTableName())
        .alterColumn(property.getColumnName())
        .set(property.fieldDataType())
        .getSQL(ParamType.INLINED);
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
          .alterTable(mapping.getTableName())
          .add(DSL.constraint(name).unique(index.columnNames)).getSQL(ParamType.INLINED);
    }

    return create()
        .createIndex(name)
        .on(mapping.getTableName(), index.columnNames).getSQL(ParamType.INLINED);
  }

  /**
   * @param index
   * @return
   */
  public String ddlDropIndex(SqlSchema.DbIndex index) {
    if (index.unique) {
      return create()
          .alterTable(index.tableName)
          .dropConstraint(index.name)
          .getSQL(ParamType.INLINED);
    }
    return create().dropIndex(index.name).getSQL(ParamType.INLINED);
  }

  /**
   * @param type
   * @return
   */
  public DataType fromJdbcType(int type, TableMapping.Property property) {
    switch (type) {
      case DBTypes.ENUM:
        return SQLDataType.VARCHAR.length(128);
      case DBTypes.NUMERIC:
        return SQLDataType.NUMERIC;
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
        if (property.columnAnnotation != null && property.columnAnnotation.precision() > 0) {
          return SQLDataType
              .DECIMAL(property.columnAnnotation.precision(), property.columnAnnotation.scale());
        } else {
          return SQLDataType.DECIMAL;
        }
      case DBTypes.DOUBLE:
        if (property.columnAnnotation != null && property.columnAnnotation.precision() > 0) {
          return SQLDataType.DOUBLE
              .precision(property.columnAnnotation.precision(), property.columnAnnotation.scale());
        } else {
          return SQLDataType.DOUBLE;
        }
      case DBTypes.FLOAT:
        if (property.columnAnnotation != null && property.columnAnnotation.precision() > 0) {
          return SQLDataType.FLOAT
              .precision(property.columnAnnotation.precision(), property.columnAnnotation.scale());
        } else {
          return SQLDataType.FLOAT;
        }
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