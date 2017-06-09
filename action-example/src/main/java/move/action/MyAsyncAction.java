package move.action;

import javax.inject.Inject;

/**
 *
 */
@InternalAction
@ActionConfig(maxExecutionMillis = 500)
public class MyAsyncAction extends AbstractAsyncAction<String, String> {
    @Inject
    public MyAsyncAction() {
    }

    @Override
    protected void start(String request) {
        respond(getRequest() + " - Back at You");
    }
}
