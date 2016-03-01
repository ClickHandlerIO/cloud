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

    public RemoteAction getRemoteAction() {
        return remoteAction;
    }

    @Override
    protected void init() {
        super.init();
        remoteAction = getActionClass().getAnnotation(RemoteAction.class);
    }

    public boolean isGuarded() {
        return remoteAction.guarded();
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
            null,
            request,
            create()
        );
    }
}
