package io.clickhandler.sql.evolution;

/**
 *
 */
public enum ChangeType {
    CREATE_TABLE,
    CREATE_UNIQUE_INDEX,
    DROP_TABLE,
    ADD_COLUMN,
    MODIFY_COLUMN,
    DROP_COLUMN,
    CREATE_INDEX,
    DROP_INDEX,
    DROP_UNIQUE_INDEX,
    CREATE_PRIMARY_KEY,;
}
