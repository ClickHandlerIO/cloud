package move.action;

import io.vertx.core.Handler;
import io.vertx.rxjava.core.Context;
import io.vertx.rxjava.core.Vertx;
import rx.Scheduler;

/**
 *
 */
public class ActionContext {
    private Vertx vertx;
    private Context context;
    private Scheduler scheduler;

    public Vertx getVertx() {
        return vertx;
    }

    void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    public Context getContext() {
        return context;
    }

    void setContext(Context context) {
        this.context = context;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void runOnContext(Handler<Void> action) {
        vertx.runOnContext(action);
    }

//    protected <A extends Action<IN, OUT>, IN, OUT> Observable<OUT> observe(
//        InternalActionProvider<A, IN, OUT> actionProvider,
//        IN request
//    ) {
//
//    }
//
//    <A extends Action<IN, OUT>, IN, OUT> Observable<OUT> observe(
//        RemoteActionProvider<A, IN, OUT> actionProvider,
//        IN request
//    );
//
//    <A extends Action<IN, OUT>, IN, OUT> Observable<OUT> observe(
//        QueueActionProvider<A, IN, OUT> actionProvider,
//        IN request
//    );
//
//    <A extends InternalActorAction<ACTOR, IN, OUT>, ACTOR extends AbstractActor, IN, OUT> Observable<OUT> ask(
//        InternalActionProvider<A, IN, OUT> actionProvider,
//        IN request
//    );
//
//    <A extends Action<IN, OUT>, ACTOR extends AbstractActor, IN, OUT> Observable<OUT> ask(
//        ActorActionProvider<A, ACTOR, IN, OUT> actionProvider,
//        IN request
//    );
}
