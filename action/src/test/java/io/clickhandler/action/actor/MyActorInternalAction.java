package io.clickhandler.action.actor;

import io.clickhandler.action.AbstractInternalActorAction;
import io.clickhandler.action.ActionConfig;
import io.clickhandler.action.InternalAction;

import javax.inject.Inject;
import java.util.UUID;

/**
 *
 */
@InternalAction
@ActionConfig(maxExecutionMillis = 2000)
public class MyActorInternalAction extends AbstractInternalActorAction<MyActor, MyActorInternalAction.Request, MyActorInternalAction.Response> {
    @Inject
    public MyActorInternalAction() {
    }


    @Override
    protected void start(MyActorInternalAction.Request request) {
        respond(new Response(UUID.randomUUID().toString()));
    }

    public static class Request {
        @Inject
        public Request() {
        }
    }

    public static class Response {
        public String uid;

        @Inject
        public Response() {
        }

        public Response(String uid) {
            this.uid = uid;
        }
    }
}
