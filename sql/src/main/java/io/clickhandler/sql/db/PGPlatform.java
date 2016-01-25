package io.clickhandler.sql.db;

import org.jooq.Configuration;
import org.jooq.SQLDialect;

/**
 *
 */
public class PGPlatform extends DatabasePlatform {
    public PGPlatform(Configuration configuration, DbConfig configEntity) {
        super(configuration, configEntity);
    }

    public SQLDialect dialect() {
        return SQLDialect.POSTGRES;
    }
}