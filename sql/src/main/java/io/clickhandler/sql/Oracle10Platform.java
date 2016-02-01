package io.clickhandler.sql;

import org.jooq.Configuration;

/**
 * Oracle10 and greater specific app.
 */
public class Oracle10Platform extends SqlPlatform {
    public Oracle10Platform(Configuration configuration, SqlConfig configEntity) {
        super(configuration, configEntity);
    }
}