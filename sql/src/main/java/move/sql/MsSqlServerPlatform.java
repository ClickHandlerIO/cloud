package move.sql;

import org.jooq.Configuration;

/**
 * Microsoft SQL Server 2005 specific app.
 */
public class MsSqlServerPlatform extends SqlPlatform {

  public MsSqlServerPlatform(Configuration configuration, SqlConfig configEntity) {
    super(configuration, configEntity);
  }
}
