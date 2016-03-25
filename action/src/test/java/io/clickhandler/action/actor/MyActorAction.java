package io.clickhandler.action.actor;

import io.clickhandler.action.AbstractActorAction;
import io.clickhandler.action.ActorAction;
import javaslang.collection.Set;

import javax.inject.Inject;

/**
 *
 */
@ActorAction(actor = MyActor.class)
public class MyActorAction extends AbstractActorAction<MyActor, MyActorAction.Request, MyActorAction.Response> {
    @Inject
    public MyActorAction() {
    }

    @Override
    protected void start(Request request) {
        actor().load().subscribe(
            result -> respond(new Response()
                .threadName(Thread.currentThread().getName())
                .watchers(actor().state().addWatcher(result))),
            e -> {
            }
        );
    }

    public static class Request {
        @Inject
        public Request() {
        }
    }

    public static class Response {
        private String threadName;
        private Set<String> watchers;

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

        public Set<String> watchers() {
            return this.watchers;
        }

        public Response watchers(final Set<String> watchers) {
            this.watchers = watchers;
            return this;
        }
    }
}
