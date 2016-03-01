package io.clickhandler.action;

import rx.Observable;

import javax.inject.Inject;

/**
 *
 */
public class InternalActionProvider<A extends Action<IN, OUT>, IN, OUT> extends ActionProvider<A, IN, OUT> {
    private static final Object DEFAULT_CONTEXT = new Object();

    private InternalAction internalAction;

    @Inject
    public InternalActionProvider() {
    }

    @Override
    protected void init() {
        internalAction = getActionClass().getAnnotation(InternalAction.class);
        super.init();
    }

    public Observable<OUT> observe(final Object context, final Func.Run1<IN> callback) {
        return super.observe(context, callback);
    }

    public Observable<OUT> observe(final Func.Run1<IN> callback) {
        return super.observe(callback);
    }

    @Override
    public Observable<OUT> observe(Object context, IN request) {
        return super.observe(context, request);
    }

    /**
     * @param request
     * @return
     */
    public Observable<OUT> observe(final IN request) {
        return observe(
            DEFAULT_CONTEXT,
            request,
            create()
        );
    }
}
