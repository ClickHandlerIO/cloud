package io.clickhandler.action.store;

import io.clickhandler.action.AbstractActorAction;
import io.clickhandler.action.ActorAction;
import rx.Subscriber;

import javax.inject.Inject;
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

        getStore().getWatchers().add(UUID.randomUUID().toString());
    }

    public static class Request {
        @Inject
        public Request() {
        }
    }

    public static class Response {
        @Inject
        public Response() {
        }
    }
}
