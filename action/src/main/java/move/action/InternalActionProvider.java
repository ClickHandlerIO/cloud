package move.action;

import javaslang.control.Try;
import rx.Observable;

import javax.inject.Inject;
import java.util.function.Consumer;

/**
 *
 */
public class InternalActionProvider<A extends Action<IN, OUT>, IN, OUT> extends ActionProvider<A, IN, OUT> {
    private InternalAction internalAction;

    @Inject
    public InternalActionProvider() {
    }

    public InternalAction getInternalAction() {
        return internalAction;
    }

    @Override
    protected void init() {
        internalAction = getActionClass().getAnnotation(InternalAction.class);
        super.init();
    }

    @Override
    public OUT execute(Try.CheckedConsumer<IN> callable) {
        return super.execute(callable);
    }

    @Override
    public OUT execute(IN request) {
        return super.execute(request);
    }

    public Observable<OUT> observe(final Consumer<IN> callback) {
        return super.observe(callback);
    }

    /**
     * @param request
     * @return
     */
    public Observable<OUT> observe(final IN request) {
        return observe(
            request,
            create()
        );
    }
}
