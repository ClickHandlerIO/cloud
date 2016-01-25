package io.clickhandler.action;

import rx.Observable;

import javax.inject.Inject;

/**
 *
 */
public class RemoteActionProvider<A extends Action<IN, OUT>, IN, OUT> extends ActionProvider<A, IN, OUT> {
    private RemoteAction remoteAction;

    @Inject
    public RemoteActionProvider() {
    }

    @Override
    protected void init() {
        super.init();
        remoteAction = getActionClass().getAnnotation(RemoteAction.class);
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
            getActionProvider().get()
        );
    }
}
