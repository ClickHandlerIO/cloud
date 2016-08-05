package io.clickhandler.action;

import com.netflix.hystrix.*;
import io.vertx.core.Context;
import io.vertx.rxjava.core.Vertx;
import javaslang.control.Try;
import rx.Observable;

import javax.annotation.Nullable;
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

    private io.vertx.core.Vertx vertxCore;
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
    private int maxConcurrentRequests;

    private boolean inSet;
    private boolean outSet;

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

    public boolean isExecutionTimeoutEnabled() {
        return executionTimeoutEnabled;
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

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public Provider<IN> getInProvider() {
        return inProvider;
    }

    @Inject
    void setInProvider(@Nullable Provider<IN> inProvider) {
        this.inProvider = inProvider;
        this.inSet = true;
        final IN in = inProvider != null ? inProvider.get() : null;
        this.inClass = in != null ? (Class<IN>) in.getClass() : null;

        if (actionProvider != null && outSet) {
            init();
        }
    }

    public Provider<OUT> getOutProvider() {
        return outProvider;
    }

    @Inject
    void setOutProvider(@Nullable Provider<OUT> outProvider) {
        this.outProvider = outProvider;
        this.outSet = true;
        final OUT out = outProvider != null ? outProvider.get() : null;
        this.outClass = out != null ? (Class<OUT>) out.getClass() : null;

        if (actionProvider != null && inSet) {
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

    public int getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    protected void init() {
        inited = true;

        vertxCore = (io.vertx.core.Vertx) vertx.getDelegate();

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

        if (actionConfig != null && actionConfig.maxConcurrentRequests() > 0) {
            maxConcurrentRequests = actionConfig.maxConcurrentRequests();
            commandPropertiesDefaults.withExecutionIsolationSemaphoreMaxConcurrentRequests(actionConfig.maxConcurrentRequests());
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
                    .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey(actionConfig, actionClass)));
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
                    .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey(actionConfig, actionClass)))
                    .andCommandPropertiesDefaults(commandPropertiesDefaults);

        }
    }

    protected String commandKey(ActionConfig config, Class actionClass) {
        if (config != null) {
            if (config.commandKey().isEmpty())
                return actionClass.getName();
            else
                return config.commandKey();
        }

        return actionClass.getName();
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

    /**
     * @param context
     * @param callback
     * @return
     */
    protected Observable<OUT> observe(final Object context, final Func.Run1<IN> callback) {
        final IN in = inProvider.get();
        if (callback != null) {
            callback.run(in);
        }
        return observe(context, in);
    }

    /**
     * @param callback
     * @return
     */
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

    /**
     * @param context
     * @param request
     * @return
     */
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
        final Observable<OUT> observable = abstractAction.toObservable();

        final Context ctx = vertxCore.getOrCreateContext();

        return Observable.create(subscriber -> {
            observable.subscribe(
                r -> {
                    if (subscriber.isUnsubscribed())
                        return;

                    ctx.runOnContext(a -> {
                        if (subscriber.isUnsubscribed())
                            return;

                        subscriber.onNext(r);
                        subscriber.onCompleted();
                    });
                },
                e -> {
                    if (subscriber.isUnsubscribed())
                        return;

                    ctx.runOnContext(a -> {
                        if (subscriber.isUnsubscribed())
                            return;

                        subscriber.onError(e);
                    });
                }
            );
        });
    }
}
