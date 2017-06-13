//package move.action;
//
//import com.netflix.hystrix.HystrixObservableCommand;
//import io.vertx.core.Context;
//import io.vertx.core.Vertx;
//import rx.Observable;
//import rx.Single;
//import rx.SingleSubscriber;
//
//import java.util.function.Consumer;
//
///**
// * @author Clay Molocznik
// */
//public abstract class AbstractAsyncAction<IN, OUT>
//    extends BaseAsyncAction<IN, OUT> {
//
//    private HystrixObservableCommand<OUT> command;
//    private HystrixObservableCommand.Setter setter;
//    private SingleSubscriber<? super OUT> subscriber;
//    private Context ctx;
//    private boolean fallback;
//
//    public boolean isFallback() {
//        return fallback;
//    }
//
//    protected HystrixObservableCommand.Setter getCommandSetter() {
//        return setter;
//    }
//
//    void configureCommand(HystrixObservableCommand.Setter setter) {
//        this.setter = setter;
//    }
//
//    /**
//     * @return
//     */
//    protected HystrixObservableCommand<OUT> build() {
//        return new HystrixObservableCommand<OUT>(getCommandSetter()) {
//            @Override
//            protected Observable<OUT> construct() {
//                return AbstractAsyncAction.this.construct();
//            }
//
//            @Override
//            protected Observable<OUT> resumeWithFallback() {
//                return constructFallback();
//            }
//        };
//    }
//
//    /**
//     * @return
//     */
//    protected Observable<OUT> construct() {
//        return Single.<OUT>create(subscriber -> {
//            try {
//                ctx = Vertx.currentContext();
//                if (!subscriber.isUnsubscribed()) {
//                    Companion.getContextLocal().set(actionContext());
//                    try {
//                        start(subscriber);
//                    } finally {
//                        Companion.getContextLocal().remove();
//                    }
//                }
//            } catch (Exception e) {
//                Companion.getContextLocal().set(actionContext());
//                try {
//                    subscriber.onError(e);
//                } finally {
//                    Companion.getContextLocal().remove();
//                }
//            }
//        }).toObservable();
//    }
//
//    protected Observable<OUT> constructFallback() {
//        return Single.<OUT>create(subscriber -> {
//            try {
//                ctx = Vertx.currentContext();
//                if (!subscriber.isUnsubscribed()) {
//                    Companion.getContextLocal().set(actionContext());
//                    try {
//                        startFallback(subscriber);
//                    } finally {
//                        Companion.getContextLocal().remove();
//                    }
//                }
//            } catch (Exception e) {
//                Companion.getContextLocal().set(actionContext());
//                try {
//                    subscriber.onError(e);
//                } finally {
//                    Companion.getContextLocal().remove();
//                }
//            }
//        }).toObservable();
//    }
//
//    /**
//     * @return
//     */
//    final HystrixObservableCommand<OUT> command() {
//        if (command != null) {
//            return command;
//        }
//        command = build();
//        return command;
//    }
//
//    /**
//     * @param subscriber
//     */
//    protected void start(SingleSubscriber<? super OUT> subscriber) {
//        this.subscriber = subscriber;
//        start(getRequest());
//    }
//
//    /**
//     * @param subscriber
//     */
//    protected void startFallback(SingleSubscriber<? super OUT> subscriber) {
//        this.subscriber = subscriber;
//        this.fallback = true;
//        startFallback(getRequest());
//    }
//
//    /**
//     * @param request
//     */
//    protected abstract void start(IN request);
//
//    /**
//     * @param request
//     */
//    protected void startFallback(IN request) {
//        throw new ActionFallbackException();
//    }
//
//    /**
//     * @param response
//     */
//    protected void respond(OUT response) {
//        complete(response);
//    }
//
//    /**
//     * @param response
//     */
//    protected void complete(OUT response) {
//        if (subscriber != null) {
//            complete(subscriber, response);
//        }
//    }
//
//    /**
//     * @param subscriber
//     * @param response
//     */
//    protected void complete(SingleSubscriber<? super OUT> subscriber, OUT response) {
//        try {
//            if (!subscriber.isUnsubscribed()) {
//                subscriber.onSuccess(response);
//            }
//        } catch (Exception e) {
//            subscriber.onError(e);
//        }
//    }
//}
