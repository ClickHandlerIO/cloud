package move.sql

import kotlinx.coroutines.experimental.rx1.await
import org.jooq.DSLContext

suspend fun <T> SqlDatabase.awaitRead(callable: (SqlSession) -> T): T {
    return rxSingle(callable).await()
}

suspend fun <T> SqlDatabase.awaitDSL(callable: (DSLContext) -> T): T {
    return rxSingle {
        callable(it.create())
    }.await()
}

suspend fun <T> SqlDatabase.awaitWrite(callable: (SqlSession) -> SqlResult<T>): SqlResult<T> {
    return rxWrite(callable).await()
}