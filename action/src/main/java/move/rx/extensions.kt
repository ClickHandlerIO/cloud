package move.rx

import kotlinx.coroutines.experimental.rx1.await
import move.*
import rx.Scheduler
import rx.Single
import rx.functions.Func2


suspend fun <T1, T2> parallel(s1: Single<out T1>,
                              s2: Single<out T2>) =
   Single.zip(s1, s2) { t1, t2 -> KTuple2(t1, t2) }.await()

suspend fun <T1, T2, T3> parallel(s1: Single<out T1>,
                                  s2: Single<out T2>,
                                  s3: Single<out T3>): KTuple3<T1, T2, T3> =
   Single.zip(
      s1,
      s2,
      s3,
      { t1, t2, t3 -> KTuple3(t1, t2, t3) }
   ).await()

suspend fun <T1, T2, T3, T4> parallel(s1: Single<out T1>,
                                      s2: Single<out T2>,
                                      s3: Single<out T3>,
                                      s4: Single<out T4>) =
   Single.zip(
      s1,
      s2,
      s3,
      s4,
      { t1, t2, t3, t4 -> KTuple4(t1, t2, t3, t4) }
   ).await()

suspend fun <T1, T2, T3, T4, T5> parallel(s1: Single<out T1>,
                                          s2: Single<out T2>,
                                          s3: Single<out T3>,
                                          s4: Single<out T4>,
                                          s5: Single<out T5>) =
   Single.zip(s1, s2, s3, s4, s5, { t1, t2, t3, t4, t5 -> KTuple5(t1, t2, t3, t4, t5) }).await()

suspend fun <T1, T2, T3, T4, T5, T6> parallel(s1: Single<out T1>,
                                              s2: Single<out T2>,
                                              s3: Single<out T3>,
                                              s4: Single<out T4>,
                                              s5: Single<out T5>,
                                              s6: Single<out T6>) =
   Single.zip(
      s1,
      s2,
      s3,
      s4,
      s5,
      s6,
      { t1, t2, t3, t4, t5, t6 -> KTuple6(t1, t2, t3, t4, t5, t6) }
   ).await()

suspend fun <T1, T2, T3, T4, T5, T6, T7> parallel(s1: Single<out T1>,
                                                  s2: Single<out T2>,
                                                  s3: Single<out T3>,
                                                  s4: Single<out T4>,
                                                  s5: Single<out T5>,
                                                  s6: Single<out T6>,
                                                  s7: Single<out T7>) =
   Single.zip(
      s1,
      s2,
      s3,
      s4,
      s5,
      s6,
      s7,
      { t1, t2, t3, t4, t5, t6, t7 -> KTuple7(t1, t2, t3, t4, t5, t6, t7) }
   ).await()

suspend fun <T1, T2, T3, T4, T5, T6, T7, T8> parallel(s1: Single<out T1>,
                                                      s2: Single<out T2>,
                                                      s3: Single<out T3>,
                                                      s4: Single<out T4>,
                                                      s5: Single<out T5>,
                                                      s6: Single<out T6>,
                                                      s7: Single<out T7>,
                                                      s8: Single<out T8>) =
   Single.zip(
      s1,
      s2,
      s3,
      s4,
      s5,
      s6,
      s7,
      s8,
      { t1, t2, t3, t4, t5, t6, t7, t8 -> KTuple8(t1, t2, t3, t4, t5, t6, t7, t8) }
   ).await()

suspend fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> parallel(s1: Single<out T1>,
                                                          s2: Single<out T2>,
                                                          s3: Single<out T3>,
                                                          s4: Single<out T4>,
                                                          s5: Single<out T5>,
                                                          s6: Single<out T6>,
                                                          s7: Single<out T7>,
                                                          s8: Single<out T8>,
                                                          s9: Single<out T9>) =
   Single.zip(
      s1,
      s2,
      s3,
      s4,
      s5,
      s6,
      s7,
      s8,
      s9,
      { t1, t2, t3, t4, t5, t6, t7, t8, t9 -> KTuple9(t1, t2, t3, t4, t5, t6, t7, t8, t9) }
   ).await()

suspend fun <T1, T2> ordered(s1: Single<out T1>, s2: Single<out T2>) =
   MoreSingles.ordered(s1, s2, { t1, t2 -> KTuple2(t1, t2) }).await()

suspend fun <T1, T2, T3> ordered(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>) =
   MoreSingles.ordered(s1, s2, s3, { t1, t2, t3 -> KTuple3(t1, t2, t3) }).await()

suspend fun <T1, T2, T3, T4> ordered(s1: Single<out T1>,
                                     s2: Single<out T2>,
                                     s3: Single<out T3>,
                                     s4: Single<out T4>) =
   MoreSingles.ordered(s1, s2, s3, s4, { t1, t2, t3, t4 -> KTuple4(t1, t2, t3, t4) }).await()

suspend fun <T1, T2, T3, T4, T5> ordered(s1: Single<out T1>,
                                         s2: Single<out T2>,
                                         s3: Single<out T3>,
                                         s4: Single<out T4>,
                                         s5: Single<out T5>) =
   MoreSingles.ordered(s1, s2, s3, s4, s5, { t1, t2, t3, t4, t5 -> KTuple5(t1, t2, t3, t4, t5) }).await()

suspend fun <T1, T2, T3, T4, T5, T6> ordered(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>, s4: Single<out T4>, s5: Single<out T5>, s6: Single<out T6>) =
   MoreSingles.ordered(s1, s2, s3, s4, s5, s6, { t1, t2, t3, t4, t5, t6 -> KTuple6(t1, t2, t3, t4, t5, t6) }).await()

suspend fun <T1, T2, T3, T4, T5, T6, T7> ordered(s1: Single<out T1>,
                                                 s2: Single<out T2>,
                                                 s3: Single<out T3>,
                                                 s4: Single<out T4>,
                                                 s5: Single<out T5>,
                                                 s6: Single<out T6>,
                                                 s7: Single<out T7>) =
   MoreSingles.ordered(s1, s2, s3, s4, s5, s6, s7, { t1, t2, t3, t4, t5, t6, t7 -> KTuple7(t1, t2, t3, t4, t5, t6, t7) }).await()

suspend fun <T1, T2, T3, T4, T5, T6, T7, T8> ordered(s1: Single<out T1>,
                                                     s2: Single<out T2>,
                                                     s3: Single<out T3>,
                                                     s4: Single<out T4>,
                                                     s5: Single<out T5>,
                                                     s6: Single<out T6>,
                                                     s7: Single<out T7>,
                                                     s8: Single<out T8>) =
   MoreSingles.ordered(s1, s2, s3, s4, s5, s6, s7, s8, { t1, t2, t3, t4, t5, t6, t7, t8 -> KTuple8(t1, t2, t3, t4, t5, t6, t7, t8) }).await()

suspend fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> ordered(s1: Single<out T1>,
                                                         s2: Single<out T2>,
                                                         s3: Single<out T3>,
                                                         s4: Single<out T4>,
                                                         s5: Single<out T5>,
                                                         s6: Single<out T6>,
                                                         s7: Single<out T7>,
                                                         s8: Single<out T8>,
                                                         s9: Single<out T9>) =
   MoreSingles.ordered(s1, s2, s3, s4, s5, s6, s7, s8, s9, { t1, t2, t3, t4, t5, t6, t7, t8, t9 -> KTuple9(t1, t2, t3, t4, t5, t6, t7, t8, t9) }).await()

//suspend fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> ordered(s1: Single<out T1>,
//                                                         s2: Single<out T2>,
//                                                         s3: Single<out T3>,
//                                                         s4: Single<out T4>,
//                                                         s5: Single<out T5>,
//                                                         s6: Single<out T6>,
//                                                         s7: Single<out T7>,
//                                                         s8: Single<out T8>,
//                                                         s9: Single<out T9>,
//                                                              s10: Single<out T10>) =
//   MoreSingles.ordered(s1, s2, s3, s4, s5, s6, s7, s8, s9, { t1, t2, t3, t4, t5, t6, t7, t8, t9 -> STuple10(t1, t2, t3, t4, t5, t6, t7, t8, t9) }).await()

data class KTuple2<T1, T2>(val _1: T1, val _2: T2)
data class KTuple3<T1, T2, T3>(val _1: T1, val _2: T2, val _3: T3)
data class KTuple4<T1, T2, T3, T4>(val _1: T1, val _2: T2, val _3: T3, val _4: T4)
data class KTuple5<T1, T2, T3, T4, T5>(val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5)
data class KTuple6<T1, T2, T3, T4, T5, T6>(val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5, val _6: T6)
data class KTuple7<T1, T2, T3, T4, T5, T6, T7>(val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5, val _6: T6, val _7: T7)
data class KTuple8<T1, T2, T3, T4, T5, T6, T7, T8>(val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5, val _6: T6, val _7: T7, val _8: T8)
data class KTuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5, val _6: T6, val _7: T7, val _8: T8, val _9: T9)
data class KTuple10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val _1: T1, val _2: T2, val _3: T3, val _4: T4, val _5: T5, val _6: T6, val _7: T7, val _8: T8, val _9: T9, val _10: T10)