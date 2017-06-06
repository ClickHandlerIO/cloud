package move.sql

import kotlinx.coroutines.experimental.rx1.await
import org.jooq.DSLContext

suspend fun <T> SqlDatabase.awaitRead(callable: (SqlSession) -> T): T {
    return readObservable(callable).toSingle().await()
}

suspend fun <T> SqlDatabase.awaitDSL(callable: (DSLContext) -> T): T {
    return readObservable{
        callable(it.create())
    }.toSingle().await()
}

suspend fun <T> SqlDatabase.awaitWrite(callable: (SqlSession) -> SqlResult<T>): SqlResult<T> {
    return writeObservable(callable).toSingle().await()
}