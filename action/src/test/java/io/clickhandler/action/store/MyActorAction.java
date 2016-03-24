package io.clickhandler.action.store;

import com.google.common.collect.ImmutableList;
import io.clickhandler.action.AbstractActorAction;
import io.clickhandler.action.ActorAction;
import rx.Subscriber;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

/**
 *
 */
@ActorAction(actor = MyActor.class)
public class MyActorAction extends AbstractActorAction<MyActor, MyActorAction.Request, MyActorAction.Response> {
    @Inject
    public MyActorAction() {
    }

    @Override
    protected void start(Subscriber<? super Response> subscriber) {
        getActor().getWatchers().add(UUID.randomUUID().toString());
        subscriber.onNext(new Response().threadName(Thread.currentThread().getName()).watchers(ImmutableList.copyOf(getActor().getWatchers())));
        subscriber.onCompleted();
    }

    public static class Request {
        @Inject
        public Request() {
        }
    }

    public static class Response {
        private String threadName;
        private List<String> watchers;

        @Inject
        public Response() {
        }

        public String threadName() {
            return this.threadName;
        }

        public Response threadName(final String threadName) {
            this.threadName = threadName;
            return this;
        }

        public List<String> watchers() {
            return this.watchers;
        }

        public Response watchers(final List<String> watchers) {
            this.watchers = watchers;
            return this;
        }
    }
}
