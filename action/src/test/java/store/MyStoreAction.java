package store;

import io.clickhandler.action.AbstractStoreAction;
import rx.Subscriber;

import javax.inject.Inject;
import java.util.UUID;

/**
 *
 */
//@StoreAction(store = MyStore.class)
public class MyStoreAction extends AbstractStoreAction<MyStore, MyStoreAction.Request, MyStoreAction.Response> {
    @Inject
    public MyStoreAction() {
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
