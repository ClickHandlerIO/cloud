package move.action;

import javaslang.control.Try;
import rx.Observable;
import rx.Single;

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

    public Observable<OUT> single(final Consumer<IN> callback) {
        return observe(callback);
    }

    public Observable<OUT> observe(final Object data, final IN request) {
        final A action = create(request);

        if (action instanceof Action) {
            final ActionContext actionContext = ((Action)action).actionContext();
            if (actionContext != null) {
                actionContext.data = data;
            }
        }

        return observe0(
            request,
            action
        );
    }

    public Single<OUT> single(final Object data, final IN request) {
        return observe(data, request).toSingle();
    }

    /**
     * @param request
     * @return
     */
    public Observable<OUT> observe(final IN request) {
        return observe0(
            request,
            create()
        );
    }

    /**
     * @param request
     * @return
     */
    public Single<OUT> single(final IN request) {
        return observe0(
            request,
            create()
        ).toSingle();
    }
}
