package io.clickhandler.sql;

import org.jooq.Configuration;
import org.jooq.SQLDialect;

/**
 *
 */
public class PGPlatform extends SqlPlatform {
    public PGPlatform(Configuration configuration, SqlConfig configEntity) {
        super(configuration, configEntity);
    }

    public SQLDialect dialect() {
        return SQLDialect.POSTGRES;
    }
}