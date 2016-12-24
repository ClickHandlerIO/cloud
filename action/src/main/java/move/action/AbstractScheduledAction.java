package move.action;

/**
 *
 */
public abstract class AbstractScheduledAction extends AbstractObservableAction<Object, Object> {
    public AbstractScheduledAction() {
    }

    @Override
    protected void start(Object request) {
        start();
    }

    protected abstract void start();

    protected void done() {
        respond(null);
    }
}
