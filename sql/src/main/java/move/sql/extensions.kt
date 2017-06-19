package move.sql

import kotlinx.coroutines.experimental.rx1.await
import move.rx.parallel
import org.jooq.Record
import rx.Single

/**
 *
 */
class SqlAdapter(val sql: SqlDatabase) {
    suspend inline fun <reified T : AbstractEntity> get(vararg id: String): List<T> {
        return read { it.getEntities<T, Record>(T::class.java, id.asList()) }
    }

    suspend inline fun <reified T : AbstractEntity> get(id: List<String>): List<T> {
        return read { it.getEntities<T, Record>(T::class.java, id) }
    }

    suspend inline fun <reified T : AbstractEntity> get(id: String): T? {
        return read { it.get<T>(id) }
    }

    inline fun <reified T : AbstractEntity> SqlSession.get(id: String): T? {
        return this.getEntity(T::class.java, id)
    }

    inline fun <reified T : AbstractEntity> SqlSession.get(ids: List<String>): List<T> {
        return this.getEntities<T, Record>(T::class.java, ids)
    }

    suspend inline fun <
            reified T1 : AbstractEntity,
            reified T2 : AbstractEntity> get(
            id: String,
            id2: String): STuple2<T1?, T2?> {
        return read(
                { it.get<T1>(id) },
                { it.get<T2>(id2) }
        ).let { STuple2(it._1, it._2) }
    }

    suspend inline fun <
            reified T1 : AbstractEntity,
            reified T2 : AbstractEntity,
            reified T3 : AbstractEntity> get(
            id: String,
            id2: String,
            id3: String): STuple3<T1?, T2?, T3?> {
        return read(
                { it.get<T1>(id) },
                { it.get<T2>(id2) },
                { it.get<T3>(id3) }
        ).let { STuple3(it._1, it._2, it._3) }
    }

    suspend inline fun <
            reified T1 : AbstractEntity,
            reified T2 : AbstractEntity,
            reified T3 : AbstractEntity,
            reified T4 : AbstractEntity> get(
            id: String,
            id2: String,
            id3: String,
            id4: String): STuple4<T1?, T2?, T3?, T4?> {
        return read(
                { it.get<T1>(id) },
                { it.get<T2>(id2) },
                { it.get<T3>(id3) },
                { it.get<T4>(id4) }
        )
    }

    suspend fun <T> write(block: (SqlSession) -> SqlResult<T>): SqlResult<T> {
        return sql.write(block).await()
    }

    internal fun <T> safeRead(block: (SqlSession) -> T): Single<SResult<T>> {
        return sql.read { SResult(block(it)) }
    }

    suspend fun <T> read(block: (SqlSession) -> T): T {
        return safeRead(block).await().value
    }

    suspend fun <T1, T2> read(block1: (SqlSession) -> T1,
                              block2: (SqlSession) -> T2): STuple2<T1, T2> {
        return parallel(
                safeRead(block1),
                safeRead(block2)
        ).let {
            STuple2(it._1.value, it._2.value)
        }
    }

    suspend fun <T1, T2, T3> read(block1: (SqlSession) -> T1,
                                  block2: (SqlSession) -> T2,
                                  block3: (SqlSession) -> T3): STuple3<T1, T2, T3> {
        return parallel(
                safeRead(block1),
                safeRead(block2),
                safeRead(block3)
        ).let {
            STuple3(it._1.value, it._2.value, it._3.value)
        }
    }

    suspend fun <T1, T2, T3, T4> read(block1: (SqlSession) -> T1,
                                      block2: (SqlSession) -> T2,
                                      block3: (SqlSession) -> T3,
                                      block4: (SqlSession) -> T4): STuple4<T1, T2, T3, T4> {
        return parallel(
                safeRead(block1),
                safeRead(block2),
                safeRead(block3),
                safeRead(block4)
        ).let {
            STuple4(it._1.value, it._2.value, it._3.value, it._4.value)
        }
    }
}


data class SResult<T1>(val value: T1)

data class STuple2<T1, T2>(val _1: T1, val _2: T2)
data class STuple3<T1, T2, T3>(val _1: T1, val _2: T2, val _3: T3)
data class STuple4<T1, T2, T3, T4>(val _1: T1, val _2: T2, val _3: T3, val _4: T4)
data class STuple5<T1, T2, T3, T4, T5>(val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5)
data class STuple6<T1, T2, T3, T4, T5, T6>(val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5, val _6: T6)