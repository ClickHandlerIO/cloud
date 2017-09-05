package move.sql

import kotlinx.coroutines.experimental.rx1.await
import move.rx.await
import org.jooq.Record
import rx.Single

/**
 *
 */
class SqlAdapter(val sql: SqlDatabase) {
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

   suspend inline fun <
      reified T1 : AbstractEntity,
      reified T2 : AbstractEntity,
      reified T3 : AbstractEntity,
      reified T4 : AbstractEntity,
      reified T5 : AbstractEntity> get(
      id: String,
      id2: String,
      id3: String,
      id4: String,
      id5: String): STuple5<T1?, T2?, T3?, T4?, T5?> {
      return read(
         { it.get<T1>(id) },
         { it.get<T2>(id2) },
         { it.get<T3>(id3) },
         { it.get<T4>(id4) },
         { it.get<T5>(id5) }
      )
   }

   suspend inline fun <
      reified T1 : AbstractEntity,
      reified T2 : AbstractEntity,
      reified T3 : AbstractEntity,
      reified T4 : AbstractEntity,
      reified T5 : AbstractEntity,
      reified T6 : AbstractEntity> get(
      id: String,
      id2: String,
      id3: String,
      id4: String,
      id5: String,
      id6: String): STuple6<T1?, T2?, T3?, T4?, T5?, T6?> {
      return read(
         { it.get<T1>(id) },
         { it.get<T2>(id2) },
         { it.get<T3>(id3) },
         { it.get<T4>(id4) },
         { it.get<T5>(id5) },
         { it.get<T6>(id6) }
      )
   }

   suspend inline fun <
      reified T1 : AbstractEntity,
      reified T2 : AbstractEntity,
      reified T3 : AbstractEntity,
      reified T4 : AbstractEntity,
      reified T5 : AbstractEntity,
      reified T6 : AbstractEntity,
      reified T7 : AbstractEntity> get(
      id: String,
      id2: String,
      id3: String,
      id4: String,
      id5: String,
      id6: String,
      id7: String): STuple7<T1?, T2?, T3?, T4?, T5?, T6?, T7?> {
      return read(
         { it.get<T1>(id) },
         { it.get<T2>(id2) },
         { it.get<T3>(id3) },
         { it.get<T4>(id4) },
         { it.get<T5>(id5) },
         { it.get<T6>(id6) },
         { it.get<T7>(id7) }
      )
   }

   suspend inline fun <
      reified T1 : AbstractEntity,
      reified T2 : AbstractEntity,
      reified T3 : AbstractEntity,
      reified T4 : AbstractEntity,
      reified T5 : AbstractEntity,
      reified T6 : AbstractEntity,
      reified T7 : AbstractEntity,
      reified T8 : AbstractEntity> get(
      id: String,
      id2: String,
      id3: String,
      id4: String,
      id5: String,
      id6: String,
      id7: String,
      id8: String): STuple8<T1?, T2?, T3?, T4?, T5?, T6?, T7?, T8?> {
      return read(
         { it.get<T1>(id) },
         { it.get<T2>(id2) },
         { it.get<T3>(id3) },
         { it.get<T4>(id4) },
         { it.get<T5>(id5) },
         { it.get<T6>(id6) },
         { it.get<T7>(id7) },
         { it.get<T8>(id8) }
      )
   }

   suspend inline fun <
      reified T1 : AbstractEntity,
      reified T2 : AbstractEntity,
      reified T3 : AbstractEntity,
      reified T4 : AbstractEntity,
      reified T5 : AbstractEntity,
      reified T6 : AbstractEntity,
      reified T7 : AbstractEntity,
      reified T8 : AbstractEntity,
      reified T9 : AbstractEntity> get(
      id: String,
      id2: String,
      id3: String,
      id4: String,
      id5: String,
      id6: String,
      id7: String,
      id8: String,
      id9: String): STuple9<T1?, T2?, T3?, T4?, T5?, T6?, T7?, T8?, T9?> {
      return read(
         { it.get<T1>(id) },
         { it.get<T2>(id2) },
         { it.get<T3>(id3) },
         { it.get<T4>(id4) },
         { it.get<T5>(id5) },
         { it.get<T6>(id6) },
         { it.get<T7>(id7) },
         { it.get<T8>(id8) },
         { it.get<T9>(id9) }
      )
   }

   internal fun <T> safeRead(block: (SqlSession) -> T): Single<SResult<T>> {
      return sql.read { SResult(block(it)) }
   }

   suspend fun <T> read(block: (SqlSession) -> T): T {
      return safeRead(block).await().value
   }

   suspend fun <T1, T2> read(block1: (SqlSession) -> T1,
                             block2: (SqlSession) -> T2): STuple2<T1, T2> {
      return await(
         safeRead(block1),
         safeRead(block2)
      ).let {
         STuple2(it._1.value, it._2.value)
      }
   }

   suspend fun <T1, T2, T3> read(block1: (SqlSession) -> T1,
                                 block2: (SqlSession) -> T2,
                                 block3: (SqlSession) -> T3): STuple3<T1, T2, T3> {
      return await(
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
      return await(
         safeRead(block1),
         safeRead(block2),
         safeRead(block3),
         safeRead(block4)
      ).let {
         STuple4(it._1.value, it._2.value, it._3.value, it._4.value)
      }
   }

   suspend fun <T1, T2, T3, T4, T5> read(block1: (SqlSession) -> T1,
                                         block2: (SqlSession) -> T2,
                                         block3: (SqlSession) -> T3,
                                         block4: (SqlSession) -> T4,
                                         block5: (SqlSession) -> T5): STuple5<T1, T2, T3, T4, T5> {
      return await(
         safeRead(block1),
         safeRead(block2),
         safeRead(block3),
         safeRead(block4),
         safeRead(block5)
      ).let {
         STuple5(it._1.value, it._2.value, it._3.value, it._4.value, it._5.value)
      }
   }

   suspend fun <T1, T2, T3, T4, T5, T6> read(block1: (SqlSession) -> T1,
                                             block2: (SqlSession) -> T2,
                                             block3: (SqlSession) -> T3,
                                             block4: (SqlSession) -> T4,
                                             block5: (SqlSession) -> T5,
                                             block6: (SqlSession) -> T6): STuple6<T1, T2, T3, T4, T5, T6> {
      return await(
         safeRead(block1),
         safeRead(block2),
         safeRead(block3),
         safeRead(block4),
         safeRead(block5),
         safeRead(block6)
      ).let {
         STuple6(it._1.value, it._2.value, it._3.value, it._4.value, it._5.value, it._6.value)
      }
   }

   suspend fun <T1, T2, T3, T4, T5, T6, T7> read(block1: (SqlSession) -> T1,
                                                 block2: (SqlSession) -> T2,
                                                 block3: (SqlSession) -> T3,
                                                 block4: (SqlSession) -> T4,
                                                 block5: (SqlSession) -> T5,
                                                 block6: (SqlSession) -> T6,
                                                 block7: (SqlSession) -> T7): STuple7<T1, T2, T3, T4, T5, T6, T7> {
      return await(
         safeRead(block1),
         safeRead(block2),
         safeRead(block3),
         safeRead(block4),
         safeRead(block5),
         safeRead(block6),
         safeRead(block7)
      ).let {
         STuple7(it._1.value, it._2.value, it._3.value, it._4.value, it._5.value, it._6.value, it._7.value)
      }
   }

   suspend fun <T1, T2, T3, T4, T5, T6, T7, T8> read(block1: (SqlSession) -> T1,
                                                     block2: (SqlSession) -> T2,
                                                     block3: (SqlSession) -> T3,
                                                     block4: (SqlSession) -> T4,
                                                     block5: (SqlSession) -> T5,
                                                     block6: (SqlSession) -> T6,
                                                     block7: (SqlSession) -> T7,
                                                     block8: (SqlSession) -> T8): STuple8<T1, T2, T3, T4, T5, T6, T7, T8> {
      return await(
         safeRead(block1),
         safeRead(block2),
         safeRead(block3),
         safeRead(block4),
         safeRead(block5),
         safeRead(block6),
         safeRead(block7),
         safeRead(block8)
      ).let {
         STuple8(it._1.value, it._2.value, it._3.value, it._4.value, it._5.value, it._6.value, it._7.value, it._8.value)
      }
   }

   suspend fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> read(block1: (SqlSession) -> T1,
                                                         block2: (SqlSession) -> T2,
                                                         block3: (SqlSession) -> T3,
                                                         block4: (SqlSession) -> T4,
                                                         block5: (SqlSession) -> T5,
                                                         block6: (SqlSession) -> T6,
                                                         block7: (SqlSession) -> T7,
                                                         block8: (SqlSession) -> T8,
                                                         block9: (SqlSession) -> T9): STuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {
      return await(
         safeRead(block1),
         safeRead(block2),
         safeRead(block3),
         safeRead(block4),
         safeRead(block5),
         safeRead(block6),
         safeRead(block7),
         safeRead(block8),
         safeRead(block9)
      ).let {
         STuple9(it._1.value, it._2.value, it._3.value, it._4.value, it._5.value, it._6.value, it._7.value, it._8.value, it._9.value)
      }
   }

   suspend fun <T> write(block: (SqlSession) -> SqlResult<T>): SqlResult<T> {
      return sql.write(block).await()
   }
}


data class SResult<T1>(val value: T1)
data class STuple2<T1, T2>(val _1: T1, val _2: T2)
data class STuple3<T1, T2, T3>(val _1: T1, val _2: T2, val _3: T3)
data class STuple4<T1, T2, T3, T4>(val _1: T1, val _2: T2, val _3: T3, val _4: T4)
data class STuple5<T1, T2, T3, T4, T5>(val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5)
data class STuple6<T1, T2, T3, T4, T5, T6>(val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5, val _6: T6)
data class STuple7<T1, T2, T3, T4, T5, T6, T7>(val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5, val _6: T6, val _7: T7)
data class STuple8<T1, T2, T3, T4, T5, T6, T7, T8>(val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5, val _6: T6, val _7: T7, val _8: T8)
data class STuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5, val _6: T6, val _7: T7, val _8: T8, val _9: T9)
data class STuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5, val _6: T6, val _7: T7, val _8: T8, val _9: T9, val _10: T10)