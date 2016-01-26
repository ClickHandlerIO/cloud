package io.clickhandler.action;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixObservableCommand;
import javaslang.control.Try;
import rx.Observable;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Handles providing
 */
public class ActionProvider<A, IN, OUT> {
    private ActionConfig actionConfig;

    private HystrixCommand.Setter defaultSetter;
    private HystrixObservableCommand.Setter defaultObservableSetter;

    private Provider<A> actionProvider;
    private Provider<IN> inProvider;
    private Provider<OUT> outProvider;
    private Class<A> actionClass;
    private Class<IN> inClass;
    private Class<OUT> outClass;

    @Inject
    public ActionProvider() {
    }

    public ActionConfig getActionConfig() {
        return actionConfig;
    }

    public Provider<A> getActionProvider() {
        return actionProvider;
    }

    @Inject
    void setActionProvider(Provider<A> actionProvider) {
        this.actionProvider = actionProvider;
        this.actionClass = (Class<A>) actionProvider.get().getClass();

        if (inProvider != null && outProvider != null) {
            init();
        }
    }

    public Provider<IN> getInProvider() {
        return inProvider;
    }

    @Inject
    void setInProvider(Provider<IN> inProvider) {
        this.inProvider = inProvider;
        this.inClass = (Class<IN>) inProvider.get().getClass();

        if (actionProvider != null && outProvider != null) {
            init();
        }
    }

    public Provider<OUT> getOutProvider() {
        return outProvider;
    }

    @Inject
    void setOutProvider(Provider<OUT> outProvider) {
        this.outProvider = outProvider;
        this.outClass = (Class<OUT>) outProvider.get().getClass();

        if (actionProvider != null && inProvider != null) {
            init();
        }
    }

    public Class<A> getActionClass() {
        return actionClass;
    }

    public Class<IN> getInClass() {
        return inClass;
    }

    public Class<OUT> getOutClass() {
        return outClass;
    }

    protected void init() {
        actionConfig = actionClass.getAnnotation(ActionConfig.class);

        if (ObservableAction.class.isAssignableFrom(actionClass)) {
            // Build HystrixObservableCommand.Setter default.
            final String groupKey = actionConfig != null ? actionConfig.groupKey() : "";
            defaultObservableSetter =
                HystrixObservableCommand.Setter
                    .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(actionClass.getName()));
        } else if (AbstractBlockingAction.class.isAssignableFrom(actionClass)) {
            // Build HystrixCommand.Setter default.
            final String groupKey = actionConfig != null ? actionConfig.groupKey() : "";
            defaultSetter =
                HystrixCommand.Setter
                    .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(actionClass.getName()));

        }
    }

    protected A create() {
        final A action = actionProvider.get();
        if (action instanceof AbstractBlockingAction) {
            ((AbstractBlockingAction)action).setCommandSetter(defaultSetter);
        } else if (action instanceof AbstractObservableAction) {
            ((AbstractObservableAction)action).setCommandSetter(defaultObservableSetter);
        }
        return action;
    }


    /**
     * @return
     */
    protected OUT execute(final Try.CheckedConsumer<IN> callable) {
        final IN in = inProvider.get();
        Try.run(() -> callable.accept(in));
        return execute(in);
    }

    /**
     * @param request
     * @return
     */
    protected A create(
        final IN request) {
        A action = create();

        final AbstractAction<IN, OUT> abstractAction = (AbstractAction<IN, OUT>) action;
        abstractAction.setRequest(request);

        return action;
    }

    /**
     * @param request
     * @return
     */
    protected OUT execute(final IN request) {
        return Try.of(() -> observe(
            request,
            create()
        ).toBlocking().toFuture().get()).get();
    }

    protected Observable<OUT> observe(final Func.Run1<IN> callback) {
        final IN in = inProvider.get();
        if (callback != null) {
            callback.run(in);
        }
        return observe(in);
    }

    /**
     * @param request
     * @return
     */
    protected Observable<OUT> observe(
        final IN request) {
        return observe(
            request,
            create()
        );
    }

    /**
     * @param request\
     */
    protected Observable<OUT> observe(
        final IN request,
        final A action) {
        final AbstractAction<IN, OUT> abstractAction;
        try {
            abstractAction = (AbstractAction<IN, OUT>) action;
            abstractAction.setRequest(request);
        } catch (Exception e) {
            // Ignore.
            return Observable.error(e);
        }

        return abstractAction.observe();
    }

    /**
     * @param request
     * @param action
     * @return
     */
    protected Observable<Boolean> queue(final IN request, final A action) {
        return Observable.empty();
    }
}
