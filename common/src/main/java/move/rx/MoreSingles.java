package move.rx;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import move.Tuple;
import move.Tuple2;
import move.Tuple3;
import move.Tuple4;
import move.Tuple5;
import move.Tuple6;
import move.Tuple7;
import move.Tuple8;
import move.Tuple9;
import rx.Scheduler;
import rx.Single;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.functions.Func5;
import rx.functions.Func6;
import rx.functions.Func7;
import rx.functions.Func8;
import rx.functions.Func9;
import rx.functions.FuncN;

/**
 *
 */
public interface MoreSingles {

  /**
   * Returns a Single that emits the results of a specified combiner function applied to two items
   * emitted by two other Singles. <p> <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png"
   * alt=""> <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a
   * particular {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2> Single<Tuple2<T1, T2>> zip(Single<? extends T1> s1,
      Single<? extends T2> s2) {
    return Single.zip(s1, s2, Tuple::of);
  }

  /**
   * Returns a Single that emits the results of a specified combiner function applied to three items
   * emitted by three other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3> Single<Tuple3<T1, T2, T3>> zip(Single<? extends T1> s1,
      Single<? extends T2> s2, Single<? extends T3> s3) {
    return Single.zip(s1, s2, s3, Tuple::of);
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to four
   * items emitted by four other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4> Single<Tuple4<T1, T2, T3, T4>> zip(Single<? extends T1> s1,
      Single<? extends T2> s2, Single<? extends T3> s3, Single<? extends T4> s4) {
    return Single.zip(s1, s2, s3, s4, Tuple::of);
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to five
   * items emitted by five other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <T5> the fifth source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param s5 a fifth source Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, T5> Single<Tuple5<T1, T2, T3, T4, T5>> zip(Single<? extends T1> s1,
      Single<? extends T2> s2, Single<? extends T3> s3, Single<? extends T4> s4,
      Single<? extends T5> s5) {
    return Single.zip(s1, s2, s3, s4, s5, Tuple::of);
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to six
   * items emitted by six other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <T5> the fifth source Single's value type
   * @param <T6> the sixth source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param s5 a fifth source Single
   * @param s6 a sixth source Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, T5, T6> Single<Tuple6<T1, T2, T3, T4, T5, T6>> zip(
      Single<? extends T1> s1, Single<? extends T2> s2, Single<? extends T3> s3,
      Single<? extends T4> s4, Single<? extends T5> s5, Single<? extends T6> s6) {
    return Single.zip(s1, s2, s3, s4, s5, s6, Tuple::of);
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to seven
   * items emitted by seven other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <T5> the fifth source Single's value type
   * @param <T6> the sixth source Single's value type
   * @param <T7> the seventh source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param s5 a fifth source Single
   * @param s6 a sixth source Single
   * @param s7 a seventh source Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, T5, T6, T7> Single<Tuple7<T1, T2, T3, T4, T5, T6, T7>> zip(
      Single<? extends T1> s1, Single<? extends T2> s2, Single<? extends T3> s3,
      Single<? extends T4> s4, Single<? extends T5> s5, Single<? extends T6> s6,
      Single<? extends T7> s7) {
    return Single.zip(s1, s2, s3, s4, s5, s6, s7, Tuple::of);
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to eight
   * items emitted by eight other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <T5> the fifth source Single's value type
   * @param <T6> the sixth source Single's value type
   * @param <T7> the seventh source Single's value type
   * @param <T8> the eighth source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param s5 a fifth source Single
   * @param s6 a sixth source Single
   * @param s7 a seventh source Single
   * @param s8 an eighth source Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, T5, T6, T7, T8> Single<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> zip(
      Single<? extends T1> s1, Single<? extends T2> s2, Single<? extends T3> s3,
      Single<? extends T4> s4, Single<? extends T5> s5, Single<? extends T6> s6,
      Single<? extends T7> s7, Single<? extends T8> s8) {
    return Single.zip(s1, s2, s3, s4, s5, s6, s7, s8, Tuple::of);
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to nine
   * items emitted by nine other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <T5> the fifth source Single's value type
   * @param <T6> the sixth source Single's value type
   * @param <T7> the seventh source Single's value type
   * @param <T8> the eighth source Single's value type
   * @param <T9> the ninth source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param s5 a fifth source Single
   * @param s6 a sixth source Single
   * @param s7 a seventh source Single
   * @param s8 an eighth source Single
   * @param s9 a ninth source Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Single<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> zip(
      Single<? extends T1> s1, Single<? extends T2> s2, Single<? extends T3> s3,
      Single<? extends T4> s4, Single<? extends T5> s5, Single<? extends T6> s6,
      Single<? extends T7> s7, Single<? extends T8> s8,
      Single<? extends T9> s9) {
    return Single.zip(s1, s2, s3, s4, s5, s6, s7, s8, s9, Tuple::of);
  }

  /**
   * Returns a Single that emits the result of specified combiner function applied to combination of
   * items emitted, in sequence, by an Iterable of other Singles.
   * <p>
   * {@code zip} applies this function in strict sequence.
   * <p>
   * <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png" alt="">
   * <dl>
   *  <dt><b>Scheduler:</b></dt>
   *  <dd>{@code zip} does not operate by default on a particular {@link Scheduler}.</dd>
   * </dl>
   *
   * @param <R> the result value type
   * @param singles
   *            an Iterable of source Singles. Should not be empty because {@link Single} either emits result or error.
   *            {@link java.util.NoSuchElementException} will be emit as error if Iterable will be empty.
   * @param zipFunction
   *            a function that, when applied to an item emitted by each of the source Singles, results in
   *            an item that will be emitted by the resulting Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators documentation: Zip</a>
   */
//    @SuppressWarnings("unchecked")
//    public static <R> Single<R> zip(Iterable<? extends Single<?>> singles, FuncN<? extends R> zipFunction) {
//        @SuppressWarnings("rawtypes")
//        Single[] iterableToArray = iterableToArray(singles);
//        return SingleOperatorZip.zip(iterableToArray, zipFunction);
//    }

  /**
   * Returns a Single that emits the results of a specified combiner function applied to two items
   * emitted by two other Singles. <p> <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png"
   * alt=""> <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a
   * particular {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <R> the result value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param zipFunction a function that, when applied to the item emitted by each of the source
   * Singles, results in an item that will be emitted by the resulting Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, R> Single<R> ordered(Single<? extends T1> s1, Single<? extends T2> s2,
      final Func2<? super T1, ? super T2, ? extends R> zipFunction) {
    return SingleOperatorOrderedZip.zip(new Single[]{s1, s2}, new FuncN<R>() {
      @Override
      public R call(Object... args) {
        return zipFunction.call((T1) args[0], (T2) args[1]);
      }
    });
  }

  /**
   * Returns a Single that emits the results of a specified combiner function applied to two items
   * emitted by two other Singles. <p> <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png"
   * alt=""> <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a
   * particular {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2> Single<Tuple2<T1, T2>> ordered(Single<? extends T1> s1,
      Single<? extends T2> s2) {
    return ordered(s1, s2, Tuple::of);
  }

  /**
   * Returns a Single that emits the results of a specified combiner function applied to three items
   * emitted by three other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <R> the result value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param zipFunction a function that, when applied to the item emitted by each of the source
   * Singles, results in an item that will be emitted by the resulting Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, R> Single<R> ordered(Single<? extends T1> s1, Single<? extends T2> s2,
      Single<? extends T3> s3,
      final Func3<? super T1, ? super T2, ? super T3, ? extends R> zipFunction) {
    return SingleOperatorOrderedZip.zip(new Single[]{s1, s2, s3}, new FuncN<R>() {
      @Override
      public R call(Object... args) {
        return zipFunction.call((T1) args[0], (T2) args[1], (T3) args[2]);
      }
    });
  }

  /**
   * Returns a Single that emits the results of a specified combiner function applied to three items
   * emitted by three other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3> Single<Tuple3<T1, T2, T3>> ordered(Single<? extends T1> s1,
      Single<? extends T2> s2, Single<? extends T3> s3) {
    return ordered(s1, s2, s3, Tuple::of);
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to four
   * items emitted by four other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <R> the result value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param zipFunction a function that, when applied to the item emitted by each of the source
   * Singles, results in an item that will be emitted by the resulting Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, R> Single<R> ordered(Single<? extends T1> s1,
      Single<? extends T2> s2, Single<? extends T3> s3, Single<? extends T4> s4,
      final Func4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipFunction) {
    return SingleOperatorOrderedZip.zip(new Single[]{s1, s2, s3, s4}, new FuncN<R>() {
      @Override
      public R call(Object... args) {
        return zipFunction.call((T1) args[0], (T2) args[1], (T3) args[2], (T4) args[3]);
      }
    });
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to four
   * items emitted by four other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single=
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4> Single<Tuple4<T1, T2, T3, T4>> ordered(Single<? extends T1> s1,
      Single<? extends T2> s2, Single<? extends T3> s3, Single<? extends T4> s4) {
    return ordered(s1, s2, s3, s4, Tuple::of);
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to five
   * items emitted by five other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <T5> the fifth source Single's value type
   * @param <R> the result value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param s5 a fifth source Single
   * @param zipFunction a function that, when applied to the item emitted by each of the source
   * Singles, results in an item that will be emitted by the resulting Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, T5, R> Single<R> ordered(Single<? extends T1> s1,
      Single<? extends T2> s2, Single<? extends T3> s3, Single<? extends T4> s4,
      Single<? extends T5> s5,
      final Func5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipFunction) {
    return SingleOperatorOrderedZip.zip(new Single[]{s1, s2, s3, s4, s5}, new FuncN<R>() {
      @Override
      public R call(Object... args) {
        return zipFunction
            .call((T1) args[0], (T2) args[1], (T3) args[2], (T4) args[3], (T5) args[4]);
      }
    });
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to five
   * items emitted by five other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <T5> the fifth source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param s5 a fifth source Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, T5> Single<Tuple5<T1, T2, T3, T4, T5>> ordered(
      Single<? extends T1> s1, Single<? extends T2> s2, Single<? extends T3> s3,
      Single<? extends T4> s4, Single<? extends T5> s5) {
    return SingleOperatorOrderedZip
        .zip(new Single[]{s1, s2, s3, s4, s5}, new FuncN<Tuple5<T1, T2, T3, T4, T5>>() {
          @Override
          public Tuple5<T1, T2, T3, T4, T5> call(Object... args) {
            return Tuple.of((T1) args[0], (T2) args[1], (T3) args[2], (T4) args[3], (T5) args[4]);
          }
        });
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to six
   * items emitted by six other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <T5> the fifth source Single's value type
   * @param <T6> the sixth source Single's value type
   * @param <R> the result value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param s5 a fifth source Single
   * @param s6 a sixth source Single
   * @param zipFunction a function that, when applied to the item emitted by each of the source
   * Singles, results in an item that will be emitted by the resulting Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, T5, T6, R> Single<R> ordered(Single<? extends T1> s1,
      Single<? extends T2> s2, Single<? extends T3> s3, Single<? extends T4> s4,
      Single<? extends T5> s5, Single<? extends T6> s6,
      final Func6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipFunction) {
    return SingleOperatorOrderedZip.zip(new Single[]{s1, s2, s3, s4, s5, s6}, new FuncN<R>() {
      @Override
      public R call(Object... args) {
        return zipFunction
            .call((T1) args[0], (T2) args[1], (T3) args[2], (T4) args[3], (T5) args[4],
                (T6) args[5]);
      }
    });
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to six
   * items emitted by six other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <T5> the fifth source Single's value type
   * @param <T6> the sixth source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param s5 a fifth source Single
   * @param s6 a sixth source Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, T5, T6> Single<Tuple6<T1, T2, T3, T4, T5, T6>> ordered(
      Single<? extends T1> s1, Single<? extends T2> s2, Single<? extends T3> s3,
      Single<? extends T4> s4, Single<? extends T5> s5, Single<? extends T6> s6) {
    return ordered(s1, s2, s3, s4, s5, s6, Tuple::of);
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to seven
   * items emitted by seven other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <T5> the fifth source Single's value type
   * @param <T6> the sixth source Single's value type
   * @param <T7> the seventh source Single's value type
   * @param <R> the result value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param s5 a fifth source Single
   * @param s6 a sixth source Single
   * @param s7 a seventh source Single
   * @param zipFunction a function that, when applied to the item emitted by each of the source
   * Singles, results in an item that will be emitted by the resulting Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, T5, T6, T7, R> Single<R> ordered(Single<? extends T1> s1,
      Single<? extends T2> s2, Single<? extends T3> s3, Single<? extends T4> s4,
      Single<? extends T5> s5, Single<? extends T6> s6, Single<? extends T7> s7,
      final Func7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipFunction) {
    return SingleOperatorOrderedZip.zip(new Single[]{s1, s2, s3, s4, s5, s6, s7}, new FuncN<R>() {
      @Override
      public R call(Object... args) {
        return zipFunction
            .call((T1) args[0], (T2) args[1], (T3) args[2], (T4) args[3], (T5) args[4],
                (T6) args[5], (T7) args[6]);
      }
    });
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to seven
   * items emitted by seven other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <T5> the fifth source Single's value type
   * @param <T6> the sixth source Single's value type
   * @param <T7> the seventh source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param s5 a fifth source Single
   * @param s6 a sixth source Single
   * @param s7 a seventh source Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, T5, T6, T7> Single<Tuple7<T1, T2, T3, T4, T5, T6, T7>> ordered(
      Single<? extends T1> s1, Single<? extends T2> s2, Single<? extends T3> s3,
      Single<? extends T4> s4, Single<? extends T5> s5, Single<? extends T6> s6,
      Single<? extends T7> s7) {
    return ordered(s1, s2, s3, s4, s5, s6, s7, Tuple::of);
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to eight
   * items emitted by eight other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <T5> the fifth source Single's value type
   * @param <T6> the sixth source Single's value type
   * @param <T7> the seventh source Single's value type
   * @param <T8> the eighth source Single's value type
   * @param <R> the result value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param s5 a fifth source Single
   * @param s6 a sixth source Single
   * @param s7 a seventh source Single
   * @param s8 an eighth source Single
   * @param zipFunction a function that, when applied to the item emitted by each of the source
   * Singles, results in an item that will be emitted by the resulting Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Single<R> ordered(Single<? extends T1> s1,
      Single<? extends T2> s2, Single<? extends T3> s3, Single<? extends T4> s4,
      Single<? extends T5> s5, Single<? extends T6> s6, Single<? extends T7> s7,
      Single<? extends T8> s8,
      final Func8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipFunction) {
    return SingleOperatorOrderedZip
        .zip(new Single[]{s1, s2, s3, s4, s5, s6, s7, s8}, new FuncN<R>() {
          @Override
          public R call(Object... args) {
            return zipFunction
                .call((T1) args[0], (T2) args[1], (T3) args[2], (T4) args[3], (T5) args[4],
                    (T6) args[5], (T7) args[6], (T8) args[7]);
          }
        });
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to eight
   * items emitted by eight other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <T5> the fifth source Single's value type
   * @param <T6> the sixth source Single's value type
   * @param <T7> the seventh source Single's value type
   * @param <T8> the eighth source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param s5 a fifth source Single
   * @param s6 a sixth source Single
   * @param s7 a seventh source Single
   * @param s8 an eighth source Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, T5, T6, T7, T8> Single<Tuple8<T1, T2, T3, T4, T5, T6, T7, T8>> ordered(
      Single<? extends T1> s1, Single<? extends T2> s2, Single<? extends T3> s3,
      Single<? extends T4> s4, Single<? extends T5> s5, Single<? extends T6> s6,
      Single<? extends T7> s7, Single<? extends T8> s8) {
    return ordered(s1, s2, s3, s4, s5, s6, s7, s8, Tuple::of);
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to nine
   * items emitted by nine other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <T5> the fifth source Single's value type
   * @param <T6> the sixth source Single's value type
   * @param <T7> the seventh source Single's value type
   * @param <T8> the eighth source Single's value type
   * @param <T9> the ninth source Single's value type
   * @param <R> the result value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param s5 a fifth source Single
   * @param s6 a sixth source Single
   * @param s7 a seventh source Single
   * @param s8 an eighth source Single
   * @param s9 a ninth source Single
   * @param zipFunction a function that, when applied to the item emitted by each of the source
   * Singles, results in an item that will be emitted by the resulting Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Single<R> ordered(Single<? extends T1> s1,
      Single<? extends T2> s2, Single<? extends T3> s3, Single<? extends T4> s4,
      Single<? extends T5> s5, Single<? extends T6> s6, Single<? extends T7> s7,
      Single<? extends T8> s8,
      Single<? extends T9> s9,
      final Func9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipFunction) {
    return SingleOperatorOrderedZip
        .zip(new Single[]{s1, s2, s3, s4, s5, s6, s7, s8, s9}, new FuncN<R>() {
          @Override
          public R call(Object... args) {
            return zipFunction
                .call((T1) args[0], (T2) args[1], (T3) args[2], (T4) args[3], (T5) args[4],
                    (T6) args[5], (T7) args[6], (T8) args[7], (T9) args[8]);
          }
        });
  }

  /**
   * Returns an Observable that emits the results of a specified combiner function applied to nine
   * items emitted by nine other Singles. <p> <img width="640" height="380"
   * src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/Single.zip.png" alt="">
   * <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a particular
   * {@link Scheduler}.</dd> </dl>
   *
   * @param <T1> the first source Single's value type
   * @param <T2> the second source Single's value type
   * @param <T3> the third source Single's value type
   * @param <T4> the fourth source Single's value type
   * @param <T5> the fifth source Single's value type
   * @param <T6> the sixth source Single's value type
   * @param <T7> the seventh source Single's value type
   * @param <T8> the eighth source Single's value type
   * @param <T9> the ninth source Single's value type
   * @param s1 the first source Single
   * @param s2 a second source Single
   * @param s3 a third source Single
   * @param s4 a fourth source Single
   * @param s5 a fifth source Single
   * @param s6 a sixth source Single
   * @param s7 a seventh source Single
   * @param s8 an eighth source Single
   * @param s9 a ninth source Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Single<Tuple9<T1, T2, T3, T4, T5, T6, T7, T8, T9>> ordered(
      Single<? extends T1> s1, Single<? extends T2> s2, Single<? extends T3> s3,
      Single<? extends T4> s4, Single<? extends T5> s5, Single<? extends T6> s6,
      Single<? extends T7> s7, Single<? extends T8> s8,
      Single<? extends T9> s9) {
    return ordered(s1, s2, s3, s4, s5, s6, s7, s8, s9, Tuple::of);
  }

  /**
   * Returns a Single that emits the result of specified combiner function applied to combination of
   * items emitted, in sequence, by an Iterable of other Singles. <p> {@code zip} applies this
   * function in strict sequence. <p> <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png"
   * alt=""> <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a
   * particular {@link Scheduler}.</dd> </dl>
   *
   * @param <R> the result value type
   * @param singles an Iterable of source Singles. Should not be empty because {@link Single} either
   * emits result or error. {@link java.util.NoSuchElementException} will be emit as error if
   * Iterable will be empty.
   * @param zipFunction a function that, when applied to an item emitted by each of the source
   * Singles, results in an item that will be emitted by the resulting Single
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <R> Single<R> ordered(Iterable<? extends Single<?>> singles,
      FuncN<? extends R> zipFunction) {
    @SuppressWarnings("rawtypes")
    Single[] iterableToArray = Iterables.toArray(singles, Single.class);
    return SingleOperatorOrderedZip.zip(iterableToArray, zipFunction);
  }

  /**
   * Returns a Single that emits the result of specified combiner function applied to combination of
   * items emitted, in sequence, by an Iterable of other Singles. <p> {@code zip} applies this
   * function in strict sequence. <p> <img width="640" height="380" src="https://raw.github.com/wiki/ReactiveX/RxJava/images/rx-operators/zip.png"
   * alt=""> <dl> <dt><b>Scheduler:</b></dt> <dd>{@code zip} does not operate by default on a
   * particular {@link Scheduler}.</dd> </dl>
   *
   * @param <R> the result value type
   * @param singles an Iterable of source Singles. Should not be empty because {@link Single} either
   * emits result or error. {@link java.util.NoSuchElementException} will be emit as error if
   * Iterable will be empty.
   * @return a Single that emits the zipped results
   * @see <a href="http://reactivex.io/documentation/operators/zip.html">ReactiveX operators
   * documentation: Zip</a>
   */
  @SuppressWarnings("unchecked")
  public static <R> Single<List<R>> orderedList(Iterable<? extends Single<R>> singles) {
    @SuppressWarnings("rawtypes")
    Single[] iterableToArray = Iterables.toArray(singles, Single.class);
    return SingleOperatorOrderedZip.zip(iterableToArray, new FuncN<List<R>>() {
      @Override
      public List<R> call(Object... args) {
        final List<R> list = new ArrayList<>(args.length);
        for (Object arg : args) {
          list.add((R) arg);
        }
        return list;
      }
    });
  }
}
