package move.action;

import javax.inject.Inject;

/**
 *
 */
@InternalAction
//@RemoteAction(path = "/some/path")
public class AllocateInventory extends AbstractObservableAction<String, String> {
    @Inject
    public AllocateInventory() {
    }

    @Override
    protected void start(String request) {
        final ActionContext actionContext = actionContext();
        String r = "T";
        System.out.println(Thread.currentThread().getName() + " - Observable Start");

        Main.vertx.executeBlocking(f -> {
            System.out.println(Thread.currentThread().getName() + " - Observable Response");
            respond("TEST2");
        }, _r-> {});
    }
}
