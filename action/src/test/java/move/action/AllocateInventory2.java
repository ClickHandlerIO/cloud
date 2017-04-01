package move.action;

import javax.inject.Inject;

/**
 *
 */
@InternalAction
@ActionConfig(threadPoolKey = "inventory")
//@RemoteAction(path = "/some/path")
public class AllocateInventory2 extends AbstractBlockingAction<String, String> {
    @Inject
    public AllocateInventory2() {
    }

    @Override
    public String handle(String request) {

        System.out.println(Thread.currentThread().getName() + " - Action Context: " + getActionContext().started);
        System.out.println(Thread.currentThread().getName() + " - Start");

        final String result = Main.actions().allocateInventory.observe("Test").toBlocking().first();

        System.out.println(Thread.currentThread().getName() + " - Returned");

        return request + " Back";
    }
}
