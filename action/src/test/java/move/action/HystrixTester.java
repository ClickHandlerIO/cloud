package move.action;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.vertx.core.Vertx;
import rx.Observable;
import rx.Subscriber;

/**
 *
 */
public class HystrixTester {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

//        HystrixCommandProperties.Setter setter = new HystrixCommandProperties.Setter();

        HystrixObservableCommand<String> command = new HystrixObservableCommand<String>(
            HystrixObservableCommand.Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("MYKEY"))
        ) {
            @Override
            protected Observable<String> construct() {
                return Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(Subscriber<? super String> subscriber) {
                        vertx.setTimer(5000, event -> {
                            subscriber.onNext("Done");
                            subscriber.onCompleted();
                        });
                    }
                });
            }
        };

        command.observe().subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(String s) {
                System.err.println(s);
            }
        });
    }
}
