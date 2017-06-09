package move.action;

/**
 *
 */
public abstract class AbstractWorkerAction<T> extends AbstractAsyncAction<T, Boolean> {
    public AbstractWorkerAction() {
    }

    protected void notProcessed() {
        respond(false);
    }

    protected void processed() {
        respond(true);
    }

    @Override
    protected void start(T request) {
        respond(handle(request));
    }

    public boolean handle(T request) {
        return false;
    }
}
