package move.action;

import rx.Observable;
import rx.Single;

import javax.inject.Inject;
import java.util.function.Consumer;

/**
 * ActionProvider for Remote Actions.
 */
public class RemoteActionProvider<A extends Action<IN, OUT>, IN, OUT> extends ActionProvider<A, IN, OUT> {
    private RemoteAction remoteAction;

    @Inject
    public RemoteActionProvider() {
    }

    /**
     * @return
     */
    public RemoteAction getRemoteAction() {
        return remoteAction;
    }

    /**
     *
     */
    @Override
    protected void init() {
        super.init();
        remoteAction = getActionClass().getAnnotation(RemoteAction.class);
    }

    /**
     * @return
     */
    public boolean isGuarded() {
        return remoteAction.guarded();
    }

    /**
     * @param callback
     * @return
     */
    public Observable<OUT> observe(final Consumer<IN> callback) {
        return super.observe(callback);
    }

    public Single<OUT> single(final Consumer<IN> callback) {
        return observe(callback).toSingle();
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

    public Single<OUT> single(final IN request) {
        return observe(request).toSingle();
    }

    public Observable<OUT> observe(final Object data, final IN request) {
        final A action = create(request);

        final ActionContext actionContext = action.actionContext();
        if (actionContext != null) {
            actionContext.data = data;
        }

        return observe0(
            request,
            action
        );
    }

    public Single<OUT> single(final Object data, final IN request) {
        return observe(data, request).toSingle();
    }

    public Observable<OUT> observe(final IN request, final Consumer<A> actionCallback) {
        final A action = create(request);
        if (actionCallback != null) {
            actionCallback.accept(action);
        }
        return observe0(request, action);
    }

    public Single<OUT> single(final IN request, final Consumer<A> actionCallback) {
        return observe(request, actionCallback).toSingle();
    }
}
