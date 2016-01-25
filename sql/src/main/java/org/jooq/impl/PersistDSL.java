package org.jooq.impl;

import org.jooq.Configuration;
import org.jooq.CreateIndexStep;
import org.jooq.Name;

/**
 *
 */
public class PersistDSL extends DSL {
    public static CreateIndexStep createUniqueIndex(Configuration configuration, String name) {
        return createUniqueIndex(configuration, name(name));
    }

    public static CreateIndexStep createUniqueIndex(Configuration configuration, Name name) {
        return new CreateUniqueIndexImpl(configuration, name);
    }
}
