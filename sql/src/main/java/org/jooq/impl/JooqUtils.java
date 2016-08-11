package org.jooq.impl;

import org.jooq.Configuration;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.jooq.Query;
import org.jooq.exception.ControlFlowSignal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

/**
 *
 */
public class JooqUtils {
    public static int[] execute(final Configuration configuration, int queryTimeout, final Collection<? extends Query> queryList) {
        final Query[] queries = queryList.toArray(new Query[queryList.size()]);
        ExecuteContext ctx = new DefaultExecuteContext(configuration, queries);
        ExecuteListener listener = new ExecuteListeners(ctx);
        Connection connection = ctx.connection();

        try {
            ctx.statement(new SettingsEnabledPreparedStatement(connection));

            String[] batchSQL = ctx.batchSQL();
            for (int i = 0; i < queries.length; i++) {
                listener.renderStart(ctx);
                batchSQL[i] = DSL.using(configuration).renderInlined(queries[i]);
                listener.renderEnd(ctx);
            }

            for (String sql : batchSQL) {
                ctx.sql(sql);
                listener.prepareStart(ctx);
                ctx.statement().addBatch(sql);
                listener.prepareEnd(ctx);
                ctx.sql(null);
            }

            listener.executeStart(ctx);

            ctx.statement().setQueryTimeout(queryTimeout);

            int[] result = ctx.statement().executeBatch();
            int[] batchRows = ctx.batchRows();
            for (int i = 0; i < batchRows.length && i < result.length; i++)
                batchRows[i] = result[i];

            listener.executeEnd(ctx);
            return result;
        }

        // [#3427] ControlFlowSignals must not be passed on to ExecuteListners
        catch (ControlFlowSignal e) {
            throw e;
        }
        catch (RuntimeException e) {
            ctx.exception(e);
            listener.exception(ctx);
            throw ctx.exception();
        }
        catch (SQLException e) {
            ctx.sqlException(e);
            listener.exception(ctx);
            throw ctx.exception();
        }
        finally {
            Tools.safeClose(listener, ctx);
        }
    }
}
