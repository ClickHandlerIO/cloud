package org.jooq.impl;

import org.jooq.*;
import org.jooq.impl.AbstractQuery;
import org.jooq.impl.QueryPartList;

import static org.jooq.Clause.CREATE_INDEX;
import static org.jooq.impl.DSL.*;

/**
 * @author Lukas Eder
 */
class CreateUniqueIndexImpl extends AbstractQuery implements

        // Cascading interface implementations for CREATE INDEX behaviour
        CreateIndexStep,
        CreateIndexFinalStep {

    /**
     * Generated UID
     */
    private static final long serialVersionUID = 8904572826501186329L;
    private static final Clause[] CLAUSES = {CREATE_INDEX};

    private final Name index;
    private Table<?> table;
    private Field<?>[] fields;

    CreateUniqueIndexImpl(Configuration configuration, Name index) {
        super(configuration);

        this.index = index;
    }

    // ------------------------------------------------------------------------
    // XXX: DSL API
    // ------------------------------------------------------------------------

    @Override
    public final CreateIndexFinalStep on(Table<?> t, Field<?>... f) {
        this.table = t;
        this.fields = f;

        return this;
    }

    @Override
    public final CreateIndexFinalStep on(String tableName, String... fieldNames) {
        Field<?>[] f = new Field[fieldNames.length];

        for (int i = 0; i < f.length; i++)
            f[i] = field(name(fieldNames[i]));

        return on(table(name(tableName)), f);
    }

    // ------------------------------------------------------------------------
    // XXX: QueryPart API
    // ------------------------------------------------------------------------

    @Override
    public final void accept(Context<?> ctx) {
        ctx.keyword("create unique index")
                .sql(' ')
                .visit(index)
                .sql(' ')
                .keyword("on")
                .sql(' ')
                .visit(table)
                .sql('(')
                .qualify(false)
                .visit(new QueryPartList<QueryPart>(fields))
                .qualify(true)
                .sql(')');
    }

    @Override
    public final Clause[] clauses(Context<?> ctx) {
        return CLAUSES;
    }
}

