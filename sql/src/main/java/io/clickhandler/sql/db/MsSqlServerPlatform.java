package io.clickhandler.sql.db;

import org.jooq.Configuration;

/**
 * Microsoft SQL Server 2005 specific app.
 */
public class MsSqlServerPlatform extends DatabasePlatform {
    public MsSqlServerPlatform(Configuration configuration, DbConfig configEntity) {
        super(configuration, configEntity);
    }
}
