package move.sql;

import java.sql.PreparedStatement;

/**
 *
 */
public interface StatementCallback<T> {
    T run(PreparedStatement statement) throws PersistException;
}
