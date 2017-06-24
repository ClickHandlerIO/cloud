package move.common;

import java.util.Queue;
import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;

/**
 *
 */
public class MoreObservables {

  public static <REQUEST, RESPONSE> Single<Boolean> singleFile(
      Queue<REQUEST> requestQueue,
      ObservableSupplier<REQUEST, RESPONSE> observableSupplier,
      OnNext<REQUEST, RESPONSE> onNext,
      OnError<REQUEST> onError) {
    return new SingleFile<REQUEST, RESPONSE>(requestQueue) {
      @Override
      protected Observable<RESPONSE> toObservable(REQUEST request) {
        return observableSupplier.apply(request);
      }

      @Override
      protected boolean onNext(REQUEST request, RESPONSE result) {
        return onNext.apply(request, result);
      }

      @Override
      protected boolean onError(REQUEST request, Throwable e) {
        return onError.apply(request, e);

      }
    }.create();
  }

  @FunctionalInterface
  public interface ObservableSupplier<T, R> {

    Observable<R> apply(T request);
  }

  @FunctionalInterface
  public interface OnNext<T, R> {

    boolean apply(T request, R result);
  }

  @FunctionalInterface
  public interface OnError<T> {

    boolean apply(T request, Throwable e);
  }

  public static abstract class SingleFile<T, R> {

    private final Queue<T> queue;
    private SingleSubscriber<? super Boolean> subscriber;

    public SingleFile(Queue<T> queue) {
      this.queue = queue;
    }

    protected abstract Observable<R> toObservable(T request);

    protected abstract boolean onNext(T request, R result);

    protected abstract boolean onError(T request, Throwable e);

    Single<Boolean> create() {
      return Single.create(subscriber -> {
        this.subscriber = subscriber;
        next();
      });
    }

    private void next() {
      if (subscriber.isUnsubscribed()) {
        return;
      }

      if (queue.isEmpty()) {
        subscriber.onSuccess(true);
        return;
      }

      final T request = queue.poll();
      if (request == null) {
        subscriber.onSuccess(true);
        return;
      }

      final Observable<R> observable = toObservable(request);

      if (observable == null) {
        subscriber.onError(new RuntimeException("toObservable returned null"));
        return;
      }

      try {
        observable.subscribe(
            r -> {
              try {
                if (onNext(request, r)) {
                  next();
                } else {
                  if (!subscriber.isUnsubscribed()) {
                    subscriber.onSuccess(false);
                  }
                }
              } catch (Throwable e2) {
                if (!subscriber.isUnsubscribed()) {
                  subscriber.onError(e2);
                }
              }
            },
            e -> {
              try {
                if (onError(request, e)) {
                  next();
                } else {
                  if (!subscriber.isUnsubscribed()) {
                    subscriber.onSuccess(false);
                  }
                }
              } catch (Throwable e2) {
                if (!subscriber.isUnsubscribed()) {
                  subscriber.onError(e2);
                }
              }
            }
        );
      } catch (Throwable e) {
        subscriber.onError(e);
      }
    }
  }
}
