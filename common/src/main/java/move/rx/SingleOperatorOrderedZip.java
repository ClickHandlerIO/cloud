package move.rx;

import java.util.NoSuchElementException;
import javaslang.control.Try;
import rx.Single;
import rx.SingleSubscriber;
import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.FuncN;

public final class SingleOperatorOrderedZip {

  /**
   * Utility class.
   */
  private SingleOperatorOrderedZip() {
    throw new IllegalStateException("No instances!");
  }

  public static <T, R> Single<R> zip(final Single<? extends T>[] singles,
      final FuncN<? extends R> zipper) {
    return Single.create(new Single.OnSubscribe<R>() {
      final Object[] values = new Object[singles.length];
      private SingleSubscriber<? super R> subscriber;
      private int index = 0;
      private SingleSubscriber<T> current;

      @Override
      public void call(final SingleSubscriber<? super R> subscriber) {
        if (singles.length == 0) {
          subscriber.onError(new NoSuchElementException("Can't zip 0 Singles."));
          return;
        }

        this.subscriber = subscriber;
        subscriber.add(new Subscription() {
          @Override
          public void unsubscribe() {
            if (current != null) {
              Try.run(() -> current.unsubscribe());
            }
          }

          @Override
          public boolean isUnsubscribed() {
            return current == null || current.isUnsubscribed();
          }
        });
        next();
      }

      private void next() {
        current = new SingleSubscriber<T>() {
          @Override
          public void onSuccess(T value) {
            current.unsubscribe();
            current = null;

            values[index] = value;
            index++;

            if (index >= singles.length) {
              R r;

              try {
                r = zipper.call(values);
              } catch (Throwable e) {
                Exceptions.throwIfFatal(e);
                onError(e);
                return;
              }

              subscriber.onSuccess(r);
            } else {
              next();
            }
          }

          @Override
          public void onError(Throwable error) {
            current.unsubscribe();
            current = null;
            subscriber.onError(error);
          }
        };

        singles[index].subscribe(current);
      }
    });
  }
}