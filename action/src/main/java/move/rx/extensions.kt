package move.rx

import kotlinx.coroutines.experimental.rx1.await
import move.*
import rx.Scheduler
import rx.Single
import rx.functions.Func2


/**
 * Returns a Single that emits the results of a specified combiner function applied to two items emitted by
 * two other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T2></T1> */
suspend fun <T1, T2> parallel(s1: Single<out T1>, s2: Single<out T2>): Tuple2<T1, T2> {
   return Single.zip(s1, s2) { t1, t2 -> Tuple.of(t1, t2) }.await()
}

/**
 * Returns a Single that emits the results of a specified combiner function applied to three items emitted
 * by three other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param <T3> the third source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @param s3   a third source Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T3></T2></T1> */
suspend fun <T1, T2, T3> parallel(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>): Tuple3<T1, T2, T3> {
   return Single.zip(s1, s2, s3, Tuple::of).await()
}

/**
 * Returns an Observable that emits the results of a specified combiner function applied to four items
 * emitted by four other Singles.
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param <T3> the third source Single's value type
 * *
 * @param <T4> the fourth source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @param s3   a third source Single
 * *
 * @param s4   a fourth source Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T4></T3></T2></T1> */
suspend fun <T1, T2, T3, T4> parallel(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>, s4: Single<out T4>): Tuple4<T1, T2, T3, T4> {
   return Single.zip(s1, s2, s3, s4, Tuple::of).await()
}

/**
 * Returns an Observable that emits the results of a specified combiner function applied to five items
 * emitted by five other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param <T3> the third source Single's value type
 * *
 * @param <T4> the fourth source Single's value type
 * *
 * @param <T5> the fifth source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @param s3   a third source Single
 * *
 * @param s4   a fourth source Single
 * *
 * @param s5   a fifth source Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T5></T4></T3></T2></T1> */
suspend fun <T1, T2, T3, T4, T5> parallel(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>, s4: Single<out T4>, s5: Single<out T5>): Tuple5<T1, T2, T3, T4, T5> {
   return Single.zip(s1, s2, s3, s4, s5, Tuple::of).await()
}

/**
 * Returns an Observable that emits the results of a specified combiner function applied to six items
 * emitted by six other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param <T3> the third source Single's value type
 * *
 * @param <T4> the fourth source Single's value type
 * *
 * @param <T5> the fifth source Single's value type
 * *
 * @param <T6> the sixth source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @param s3   a third source Single
 * *
 * @param s4   a fourth source Single
 * *
 * @param s5   a fifth source Single
 * *
 * @param s6   a sixth source Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T6></T5></T4></T3></T2></T1> */
suspend fun <T1, T2, T3, T4, T5, T6> parallel(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>, s4: Single<out T4>, s5: Single<out T5>, s6: Single<out T6>): Tuple6<T1, T2, T3, T4, T5, T6> {
   return Single.zip(s1, s2, s3, s4, s5, s6, Tuple::of).await()
}

/**
 * Returns an Observable that emits the results of a specified combiner function applied to seven items
 * emitted by seven other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param <T3> the third source Single's value type
 * *
 * @param <T4> the fourth source Single's value type
 * *
 * @param <T5> the fifth source Single's value type
 * *
 * @param <T6> the sixth source Single's value type
 * *
 * @param <T7> the seventh source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @param s3   a third source Single
 * *
 * @param s4   a fourth source Single
 * *
 * @param s5   a fifth source Single
 * *
 * @param s6   a sixth source Single
 * *
 * @param s7   a seventh source Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T7></T6></T5></T4></T3></T2></T1> */
suspend fun <T1, T2, T3, T4, T5, T6, T7> parallel(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>, s4: Single<out T4>, s5: Single<out T5>, s6: Single<out T6>, s7: Single<out T7>): Tuple7<T1, T2, T3, T4, T5, T6, T7> {
   return Single.zip(s1, s2, s3, s4, s5, s6, s7, Tuple::of).await()
}

/**
 * Returns an Observable that emits the results of a specified combiner function applied to eight items
 * emitted by eight other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param <T3> the third source Single's value type
 * *
 * @param <T4> the fourth source Single's value type
 * *
 * @param <T5> the fifth source Single's value type
 * *
 * @param <T6> the sixth source Single's value type
 * *
 * @param <T7> the seventh source Single's value type
 * *
 * @param <T8> the eighth source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @param s3   a third source Single
 * *
 * @param s4   a fourth source Single
 * *
 * @param s5   a fifth source Single
 * *
 * @param s6   a sixth source Single
 * *
 * @param s7   a seventh source Single
 * *
 * @param s8   an eighth source Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T8></T7></T6></T5></T4></T3></T2></T1> */
suspend fun <T1, T2, T3, T4, T5, T6, T7, T8> parallel(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>, s4: Single<out T4>, s5: Single<out T5>, s6: Single<out T6>, s7: Single<out T7>, s8: Single<out T8>): Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> {
   return Single.zip(s1, s2, s3, s4, s5, s6, s7, s8, Tuple::of).await()
}

/**
 * Returns an Observable that emits the results of a specified combiner function applied to nine items
 * emitted by nine other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param <T3> the third source Single's value type
 * *
 * @param <T4> the fourth source Single's value type
 * *
 * @param <T5> the fifth source Single's value type
 * *
 * @param <T6> the sixth source Single's value type
 * *
 * @param <T7> the seventh source Single's value type
 * *
 * @param <T8> the eighth source Single's value type
 * *
 * @param <T9> the ninth source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @param s3   a third source Single
 * *
 * @param s4   a fourth source Single
 * *
 * @param s5   a fifth source Single
 * *
 * @param s6   a sixth source Single
 * *
 * @param s7   a seventh source Single
 * *
 * @param s8   an eighth source Single
 * *
 * @param s9   a ninth source Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T9></T8></T7></T6></T5></T4></T3></T2></T1> */
suspend fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> parallel(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>, s4: Single<out T4>, s5: Single<out T5>, s6: Single<out T6>, s7: Single<out T7>, s8: Single<out T8>,
                                                          s9: Single<out T9>): Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {
   return Single.zip(s1, s2, s3, s4, s5, s6, s7, s8, s9, Tuple::of).await()
}

/**
 * Returns a Single that emits the results of a specified combiner function applied to two items emitted by
 * two other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1>        the first source Single's value type
 * *
 * @param <T2>        the second source Single's value type
 * *
 * @param <R>         the result value type
 * *
 * @param s1          the first source Single
 * *
 * @param s2          a second source Single
 * *
 * @param zipFunction a function that, when applied to the item emitted by each of the source Singles, results in an
 * *                    item that will be emitted by the resulting Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</R></T2></T1> */
fun <T1, T2, R> ordered(s1: Single<out T1>, s2: Single<out T2>, zipFunction: Func2<in T1, in T2, out R>): Single<R> {
   return SingleOperatorOrderedZip.zip(arrayOf(s1, s2)) { args -> zipFunction.call(args[0] as T1, args[1] as T2) }
}

/**
 * Returns a Single that emits the results of a specified combiner function applied to two items emitted by
 * two other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T2></T1> */
suspend fun <T1, T2> ordered(s1: Single<out T1>, s2: Single<out T2>): Tuple2<T1, T2> {
   return MoreSingles.ordered(s1, s2, Tuple::of).await()
}


/**
 * Returns a Single that emits the results of a specified combiner function applied to three items emitted
 * by three other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param <T3> the third source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @param s3   a third source Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T3></T2></T1> */
suspend fun <T1, T2, T3> ordered(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>): Tuple3<T1, T2, T3> {
   return MoreSingles.ordered(s1, s2, s3, Tuple::of).await()
}

/**
 * Returns an Observable that emits the results of a specified combiner function applied to four items
 * emitted by four other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param <T3> the third source Single's value type
 * *
 * @param <T4> the fourth source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @param s3   a third source Single
 * *
 * @param s4   a fourth source Single=
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T4></T3></T2></T1> */
suspend fun <T1, T2, T3, T4> ordered(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>, s4: Single<out T4>): Tuple4<T1, T2, T3, T4> {
   return MoreSingles.ordered(s1, s2, s3, s4, Tuple::of).await()
}

/**
 * Returns an Observable that emits the results of a specified combiner function applied to five items
 * emitted by five other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param <T3> the third source Single's value type
 * *
 * @param <T4> the fourth source Single's value type
 * *
 * @param <T5> the fifth source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @param s3   a third source Single
 * *
 * @param s4   a fourth source Single
 * *
 * @param s5   a fifth source Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T5></T4></T3></T2></T1> */
suspend fun <T1, T2, T3, T4, T5> ordered(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>, s4: Single<out T4>, s5: Single<out T5>): Tuple5<T1, T2, T3, T4, T5> {
   return MoreSingles.ordered(s1, s2, s3, s4, s5, Tuple::of).await()
}

/**
 * Returns an Observable that emits the results of a specified combiner function applied to six items
 * emitted by six other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param <T3> the third source Single's value type
 * *
 * @param <T4> the fourth source Single's value type
 * *
 * @param <T5> the fifth source Single's value type
 * *
 * @param <T6> the sixth source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @param s3   a third source Single
 * *
 * @param s4   a fourth source Single
 * *
 * @param s5   a fifth source Single
 * *
 * @param s6   a sixth source Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T6></T5></T4></T3></T2></T1> */
suspend fun <T1, T2, T3, T4, T5, T6> ordered(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>, s4: Single<out T4>, s5: Single<out T5>, s6: Single<out T6>): Tuple6<T1, T2, T3, T4, T5, T6> {
   return MoreSingles.ordered(s1, s2, s3, s4, s5, s6, Tuple::of).await()
}

/**
 * Returns an Observable that emits the results of a specified combiner function applied to seven items
 * emitted by seven other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param <T3> the third source Single's value type
 * *
 * @param <T4> the fourth source Single's value type
 * *
 * @param <T5> the fifth source Single's value type
 * *
 * @param <T6> the sixth source Single's value type
 * *
 * @param <T7> the seventh source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @param s3   a third source Single
 * *
 * @param s4   a fourth source Single
 * *
 * @param s5   a fifth source Single
 * *
 * @param s6   a sixth source Single
 * *
 * @param s7   a seventh source Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T7></T6></T5></T4></T3></T2></T1> */
suspend fun <T1, T2, T3, T4, T5, T6, T7> ordered(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>, s4: Single<out T4>, s5: Single<out T5>, s6: Single<out T6>, s7: Single<out T7>): Tuple7<T1, T2, T3, T4, T5, T6, T7> {
   return MoreSingles.ordered(s1, s2, s3, s4, s5, s6, s7, Tuple::of).await()
}

/**
 * Returns an Observable that emits the results of a specified combiner function applied to eight items
 * emitted by eight other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param <T3> the third source Single's value type
 * *
 * @param <T4> the fourth source Single's value type
 * *
 * @param <T5> the fifth source Single's value type
 * *
 * @param <T6> the sixth source Single's value type
 * *
 * @param <T7> the seventh source Single's value type
 * *
 * @param <T8> the eighth source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @param s3   a third source Single
 * *
 * @param s4   a fourth source Single
 * *
 * @param s5   a fifth source Single
 * *
 * @param s6   a sixth source Single
 * *
 * @param s7   a seventh source Single
 * *
 * @param s8   an eighth source Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T8></T7></T6></T5></T4></T3></T2></T1> */
suspend fun <T1, T2, T3, T4, T5, T6, T7, T8> ordered(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>, s4: Single<out T4>, s5: Single<out T5>, s6: Single<out T6>, s7: Single<out T7>, s8: Single<out T8>): Tuple8<T1, T2, T3, T4, T5, T6, T7, T8> {
   return MoreSingles.ordered(s1, s2, s3, s4, s5, s6, s7, s8, Tuple::of).await()
}

/**
 * Returns an Observable that emits the results of a specified combiner function applied to nine items
 * emitted by nine other Singles.
 *
 *
 * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt=""></img>
 * <dl>
 * <dt>**Scheduler:**</dt>
 * <dd>`zip` does not operate by default on a particular [Scheduler].</dd>
</dl> *

 * @param <T1> the first source Single's value type
 * *
 * @param <T2> the second source Single's value type
 * *
 * @param <T3> the third source Single's value type
 * *
 * @param <T4> the fourth source Single's value type
 * *
 * @param <T5> the fifth source Single's value type
 * *
 * @param <T6> the sixth source Single's value type
 * *
 * @param <T7> the seventh source Single's value type
 * *
 * @param <T8> the eighth source Single's value type
 * *
 * @param <T9> the ninth source Single's value type
 * *
 * @param s1   the first source Single
 * *
 * @param s2   a second source Single
 * *
 * @param s3   a third source Single
 * *
 * @param s4   a fourth source Single
 * *
 * @param s5   a fifth source Single
 * *
 * @param s6   a sixth source Single
 * *
 * @param s7   a seventh source Single
 * *
 * @param s8   an eighth source Single
 * *
 * @param s9   a ninth source Single
 * *
 * @return a Single that emits the zipped results
 * *
 * @see [ReactiveX operators documentation: Zip](http://reactivex.io/documentation/operators/zip.html)
</T9></T8></T7></T6></T5></T4></T3></T2></T1> */
suspend fun <T1, T2, T3, T4, T5, T6, T7, T8, T9> ordered(s1: Single<out T1>, s2: Single<out T2>, s3: Single<out T3>, s4: Single<out T4>, s5: Single<out T5>, s6: Single<out T6>, s7: Single<out T7>, s8: Single<out T8>,
                                                         s9: Single<out T9>): Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {
   return MoreSingles.ordered(s1, s2, s3, s4, s5, s6, s7, s8, s9, Tuple::of).await()
}