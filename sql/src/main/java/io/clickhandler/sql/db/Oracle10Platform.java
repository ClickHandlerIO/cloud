package io.clickhandler.sql.db;

import org.jooq.Configuration;

/**
 * Oracle10 and greater specific app.
 */
public class Oracle10Platform extends DatabasePlatform {
    public Oracle10Platform(Configuration configuration, DbConfig configEntity) {
        super(configuration, configEntity);
    }
}