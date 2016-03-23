package io.clickhandler.action;

import com.netflix.hystrix.*;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.core.Vertx;
import javaslang.control.Try;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Builds and invokes a single type of Action.
 *
 * @author Clay Molocznik
 */
public class ActionProvider<A, IN, OUT> {
    static final long DEFAULT_TIMEOUT_MILLIS = 5000;
    private final HystrixCommandProperties.Setter commandPropertiesDefaults = HystrixCommandProperties.Setter();
    @Inject
    Vertx vertx;
    Scheduler scheduler;
    private ActionConfig actionConfig;
    private HystrixCommand.Setter defaultSetter;
    private HystrixObservableCommand.Setter defaultObservableSetter;
    private Provider<A> actionProvider;
    private Provider<IN> inProvider;
    private Provider<OUT> outProvider;
    private Class<A> actionClass;
    private Class<IN> inClass;
    private Class<OUT> outClass;
    private boolean inited;
    private boolean executionTimeoutEnabled;
    private long timeoutMillis;

    @Inject
    public ActionProvider() {
    }

    public ActionConfig getActionConfig() {
        return actionConfig;
    }

    public Provider<A> getActionProvider() {
        return actionProvider;
    }

    public boolean isExecutionTimeoutEnabled() {
        return executionTimeoutEnabled;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
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

    public void setExecutionTimeoutEnabled(boolean enabled) {
        commandPropertiesDefaults.withExecutionTimeoutEnabled(enabled);

        if (!inited)
            init();

        if (defaultObservableSetter != null)
            defaultObservableSetter.andCommandPropertiesDefaults(commandPropertiesDefaults);
        else if (defaultSetter != null)
            defaultSetter.andCommandPropertiesDefaults(commandPropertiesDefaults);
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
        inited = true;

        scheduler = initScheduler();
        // Get default config.
        actionConfig = actionClass.getAnnotation(ActionConfig.class);

        // Timeout milliseconds.
        long timeoutMillis = DEFAULT_TIMEOUT_MILLIS;
        if (actionConfig != null) {
            if (actionConfig.maxExecutionMillis() == 0) {
                timeoutMillis = 0;
            } else if (actionConfig.maxExecutionMillis() > 0) {
                timeoutMillis = actionConfig.maxExecutionMillis();
            }
        }

        this.timeoutMillis = timeoutMillis;

        // Enable timeout?
        if (timeoutMillis > 0) {
            commandPropertiesDefaults.withExecutionTimeoutEnabled(true);
            commandPropertiesDefaults.withExecutionTimeoutInMilliseconds((int) timeoutMillis);
            this.executionTimeoutEnabled = true;
        }

        // Default isolation strategy.
        final ExecutionIsolationStrategy isolationStrategy = actionConfig != null
            ? actionConfig.isolationStrategy()
            : ExecutionIsolationStrategy.BEST;

        if (ObservableAction.class.isAssignableFrom(actionClass)) {
            // Determine Hystrix isolation strategy.
            HystrixCommandProperties.ExecutionIsolationStrategy hystrixIsolation;
            switch (isolationStrategy) {
                // Default to SEMAPHORE
                default:
                case SEMAPHORE:
                case BEST:
                    hystrixIsolation = HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE;
                    break;
                case THREAD:
                    hystrixIsolation = HystrixCommandProperties.ExecutionIsolationStrategy.THREAD;
                    break;
            }

            // Set Hystrix isolation strategy.
            commandPropertiesDefaults.withExecutionIsolationStrategy(hystrixIsolation);

            // Build HystrixObservableCommand.Setter default.
            final String groupKey = actionConfig != null ? actionConfig.groupKey() : "";
            defaultObservableSetter =
                HystrixObservableCommand.Setter
                    // Set Group Key
                    .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                    // Set default command props
                    .andCommandPropertiesDefaults(commandPropertiesDefaults)
                    // Set command key
                    .andCommandKey(HystrixCommandKey.Factory.asKey(actionClass.getName()));
        }

        // Is it a blocking action.
        else if (AbstractBlockingAction.class.isAssignableFrom(actionClass)) {
            HystrixCommandProperties.ExecutionIsolationStrategy hystrixIsolation;
            switch (isolationStrategy) {
                case SEMAPHORE:
                    hystrixIsolation = HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE;
                    break;
                default:
                case BEST:
                case THREAD:
                    hystrixIsolation = HystrixCommandProperties.ExecutionIsolationStrategy.THREAD;
                    break;
            }

            // Set Hystrix isolation strategy.
            commandPropertiesDefaults.withExecutionIsolationStrategy(hystrixIsolation);

            // Build HystrixCommand.Setter default.
            final String groupKey = actionConfig != null ? actionConfig.groupKey() : "";
            defaultSetter =
                HystrixCommand.Setter
                    .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(actionClass.getName()))
                    .andCommandPropertiesDefaults(commandPropertiesDefaults);

        }
    }

    protected A create() {
        final A action = actionProvider.get();
        if (action instanceof AbstractBlockingAction) {
            ((AbstractBlockingAction) action).setCommandSetter(defaultSetter);
        } else if (action instanceof AbstractObservableAction) {
            ((AbstractObservableAction) action).setCommandSetter(defaultObservableSetter);
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
            null,
            request,
            create()
        ).toBlocking().toFuture().get()).get();
    }

    protected Observable<OUT> observe(final Object context, final Func.Run1<IN> callback) {
        final IN in = inProvider.get();
        if (callback != null) {
            callback.run(in);
        }
        return observe(context, in);
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
            null,
            request,
            create()
        );
    }

    protected Observable<OUT> observe(
        final Object context,
        final IN request) {
        return observe(
            context,
            request,
            create()
        );
    }

    /**
     * @param request\
     */
    protected Observable<OUT> observe(
        final Object context,
        final IN request,
        final A action) {
        final AbstractAction<IN, OUT> abstractAction;
        try {
            abstractAction = (AbstractAction<IN, OUT>) action;
            abstractAction.setContext(context);
            abstractAction.setRequest(request);
        } catch (Exception e) {
            // Ignore.
            return Observable.error(e);
        }

        // Build observable.
        final Observable observable = abstractAction.toObservable();
        // Set the observe on scheduler.
        observable.observeOn(observeOnScheduler());
        // Set the subscribe on scheduler.
        observable.subscribeOn(subscribeOnScheduler());
        return observable;
    }

    /**
     * @param request\
     */
    protected Observable<OUT> send(
        final String address,
        final IN request) {

        // Find address.

//        final AbstractAction<IN, OUT> abstractAction;
//        try {
//            abstractAction = (AbstractAction<IN, OUT>) action;
//            abstractAction.setContext(context);
//            abstractAction.setRequest(request);
//        } catch (Exception e) {
//            // Ignore.
//            return Observable.error(e);
//        }
//
//        // Build observable.
//        final Observable observable = abstractAction.toObservable();
//        // Set the observe on scheduler.
//        observable.observeOn(observeOnScheduler());
//        // Set the subscribe on scheduler.
//        observable.subscribeOn(subscribeOnScheduler());
//        return observable;
        return Observable.just(null);
    }

    protected Scheduler observeOnScheduler() {
        return scheduler;
    }

    protected Scheduler subscribeOnScheduler() {
        return scheduler;
    }

    protected Scheduler initScheduler() {
        return RxHelper.scheduler(vertx);
    }
}
