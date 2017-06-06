package move.action;

import javaslang.control.Try;

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

//        if (true) throw new RuntimeException("Tehe!!!");

        Main.vertx.executeBlocking(f -> {
            Try.run(() -> Thread.sleep(25));
            System.out.println(Thread.currentThread().getName() + " - Observable Response");
            respond("TEST2");
        }, _r-> {});
    }
}
