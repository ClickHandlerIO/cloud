package io.clickhandler.action;

import com.google.common.base.Strings;
import io.vertx.rxjava.core.Vertx;
import rx.Observable;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Objects;

/**
 *
 */
public class ActorActionProvider<A extends Action<IN, OUT>, ACTOR extends AbstractActor, IN, OUT> extends ActionProvider<A, IN, OUT> {
    private static final Object DEFAULT_CONTEXT = new Object();
    private static final int LATENCY_OFFSET_MILLIS = 500;

    @Inject
    Vertx vertx;
    @Inject
    Provider<ACTOR> actorFactory;

    private ActorAction actorAction;
    private String name = "";
    private ActorManager actorManager;

    @Inject
    public ActorActionProvider() {
    }

    public String getName() {
        return name;
    }

    public Provider<ACTOR> getActorFactory() {
        return actorFactory;
    }

    public ActorManager getActorManager() {
        return actorManager;
    }

    void setActorManager(ActorManager actorManager) {
        this.actorManager = actorManager;
    }

    @Override
    protected void init() {
        actorAction = getActionClass().getAnnotation(ActorAction.class);
        name = Objects.toString(
            Strings.emptyToNull(actorAction.name()),
            getActionClass().getCanonicalName()
        );
        super.init();
    }

    public Observable<OUT> ask(String key, IN request) {
        return ask(key, isExecutionTimeoutEnabled() ? (int) getTimeoutMillis() + LATENCY_OFFSET_MILLIS : 0, request);
    }

    public Observable<OUT> ask(String key, int timeoutMillis, IN request) {
        return actorManager.ask(this, timeoutMillis, key, request);
    }
}
