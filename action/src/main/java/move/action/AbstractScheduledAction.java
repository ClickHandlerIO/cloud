package move.action;

/**
 *
 */
public abstract class AbstractScheduledAction extends AbstractAsyncAction<Object, Object> {
    public static final Object RESULT = new Object();

    public AbstractScheduledAction() {
    }

    @Override
    protected void start(Object request) {
        start();
    }

    protected abstract void start();

    protected void done() {
        respond(RESULT);
    }
}
