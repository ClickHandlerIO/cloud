package move.sql;

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
  }

  /**
   * @param type
   * @return
   */
  public DataType fromJdbcType(int type, TableMapping.Property property) {
    switch (type) {
      case DBTypes.ENUM:
        return NuoDBDataType.STRING;
      case DBTypes.NUMERIC:
        return NuoDBDataType.DOUBLE;
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
        return NuoDBDataType.DATE;
      case DBTypes.DECIMAL:
        return NuoDBDataType.DOUBLE;
      case DBTypes.DOUBLE:
        return NuoDBDataType.DOUBLE;
      case DBTypes.FLOAT:
        return NuoDBDataType.DOUBLE;
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
