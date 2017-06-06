package move.action;

import javax.inject.Inject;

/**
 *
 */
@InternalAction
@ActionConfig
//@RemoteAction(path = "/some/path")
public class AllocateInventory2 extends AbstractBlockingAction<String, String> {
    MyScheduledAction2 a;

    @Inject
    public AllocateInventory2() {
    }

    @Override
    public String handle(String request) {

        System.out.println(Thread.currentThread().getName() + " - Action Context: " + actionContext().started);
        System.out.println(Thread.currentThread().getName() + " - Start");

        final String result = Main.actions().allocateInventory.observe("Test").toBlocking().first();

        System.out.println(Thread.currentThread().getName() + " - Returned");

        return request + " Back";
    }
}
