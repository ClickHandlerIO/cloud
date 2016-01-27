package io.clickhandler.action;

import rx.Observable;

import javax.inject.Inject;

/**
 *
 */
public class InternalActionProvider<A extends Action<IN, OUT>, IN, OUT> extends ActionProvider<A, IN, OUT> {
    private InternalAction internalAction;

    @Inject
    public InternalActionProvider() {
    }

    @Override
    protected void init() {
        internalAction = getActionClass().getAnnotation(InternalAction.class);
        super.init();

    }

    public Observable<OUT> observe(final Func.Run1<IN> callback) {
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
