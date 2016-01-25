package io.clickhandler.sql.db;

import org.jooq.DSLContext;

/**
 *
 */
public interface DSLRunnable {
    void run(DSLContext context);
}
