package move.action;

import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableDefault;
import io.vertx.rxjava.core.Vertx;
import javaslang.control.Try;
import rx.Observable;
import rx.Single;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import java.util.function.Consumer;

/**
 * Builds and invokes a single type of Action.
 *
 * @author Clay Molocznik
 */
public class ActionProvider<A, IN, OUT> {
   static final int DEFAULT_TIMEOUT_MILLIS = 5000;
   static final HystrixRequestVariableDefault<ActionContext> providerVariable = new HystrixRequestVariableDefault<>();

   private final HystrixCommandProperties.Setter commandPropertiesDefaults = HystrixCommandProperties.Setter();
   private final HystrixThreadPoolProperties.Setter threadPoolPropertiesDefaults = HystrixThreadPoolProperties.Setter();
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
   private boolean blocking;
   private boolean inited;
   private boolean executionTimeoutEnabled;
   private int timeoutMillis;
   private int maxConcurrentRequests;
   private boolean inSet;
   private boolean outSet;

   @Inject
   public ActionProvider() {
   }

   public static ActionContext current() {
      return AbstractAction.contextLocal.get();
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

   public boolean isBlocking() {
      return blocking;
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
      int timeoutMillis = DEFAULT_TIMEOUT_MILLIS;
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
         commandPropertiesDefaults.withExecutionTimeoutInMilliseconds(timeoutMillis);
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
            // Default to THREAD
            default:
            case BEST:
               hystrixIsolation = best();
               break;
            case SEMAPHORE:
               hystrixIsolation = HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE;
               break;
            case THREAD:
               hystrixIsolation = HystrixCommandProperties.ExecutionIsolationStrategy.THREAD;
               break;
         }

         blocking = hystrixIsolation == HystrixCommandProperties.ExecutionIsolationStrategy.THREAD;

         // Set Hystrix isolation strategy.
         commandPropertiesDefaults.withExecutionIsolationStrategy(hystrixIsolation);
         commandPropertiesDefaults.withFallbackEnabled(true);

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

         if (actionConfig != null && actionConfig.maxConcurrentRequests() > 0) {
            maxConcurrentRequests = actionConfig.maxConcurrentRequests();
            commandPropertiesDefaults.withExecutionIsolationSemaphoreMaxConcurrentRequests(actionConfig.maxConcurrentRequests());
         }
      }


      // Is it a blocking action?
      else if (AbstractBlockingAction.class.isAssignableFrom(actionClass)) {
         HystrixCommandProperties.ExecutionIsolationStrategy hystrixIsolation;
         switch (isolationStrategy) {
            case SEMAPHORE:
               hystrixIsolation = HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE;
               break;
            default:
            case BEST:
               hystrixIsolation = best();
               break;
            case THREAD:
               hystrixIsolation = HystrixCommandProperties.ExecutionIsolationStrategy.THREAD;
               break;
         }

         // Set Hystrix isolation strategy.
         commandPropertiesDefaults
            .withExecutionIsolationStrategy(hystrixIsolation)
            .withCircuitBreakerEnabled(true)
            .withFallbackEnabled(true);

         final String groupKey = actionConfig != null ? actionConfig.groupKey() : "";

         ThreadPoolConfig threadPoolConfig = ActionManager.getThreadPoolConfigs().get(groupKey);

         if (threadPoolConfig == null) {
            threadPoolPropertiesDefaults
               .withCoreSize(10)
               .withAllowMaximumSizeToDivergeFromCoreSize(true)
               .withMaximumSize(25);
         } else {
            threadPoolPropertiesDefaults
               .withCoreSize(threadPoolConfig.coreSize())
               .withAllowMaximumSizeToDivergeFromCoreSize(true)
               .withMaximumSize(threadPoolConfig.maxSize());
         }

         // Build HystrixCommand.Setter default.
         defaultSetter =
            HystrixCommand.Setter
               .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
               .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(groupKey))
               .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey(actionConfig, actionClass)))
               .andCommandPropertiesDefaults(commandPropertiesDefaults)
               .andThreadPoolPropertiesDefaults(threadPoolPropertiesDefaults);
      }
   }

   protected HystrixCommandProperties.ExecutionIsolationStrategy best() {
      // Default Scheduled Actions to run in thread pools.
      if (AbstractScheduledAction.class.isAssignableFrom(actionClass)
         || AbstractBlockingScheduledAction.class.isAssignableFrom(actionClass)) {
         return HystrixCommandProperties.ExecutionIsolationStrategy.THREAD;
      }

      // Default Blocking Actions to run on a Thread Pool.
      else if (AbstractBlockingAction.class.isAssignableFrom(actionClass)) {
         return HystrixCommandProperties.ExecutionIsolationStrategy.THREAD;
      }

      // Default Worker Actions to run in thread pools.
      else if (AbstractWorkerAction.class.isAssignableFrom(actionClass)
         || AbstractBlockingWorkerAction.class.isAssignableFrom(actionClass)) {
         return HystrixCommandProperties.ExecutionIsolationStrategy.THREAD;
      }

      // Default non-blocking remote and internal Actions to run on the calling thread (vertx event loop).
      else {
         return HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE;
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
      if (action instanceof AbstractAction) {
         ActionContext context = AbstractAction.contextLocal.get();
         if (context == null) {
            context = new ActionContext(timeoutMillis, this, io.vertx.core.Vertx.currentContext());
         }
         ((AbstractAction) action).setContext(context);
      }

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
   public OUT execute(final Try.CheckedConsumer<IN> callable) {
      final IN in = inProvider.get();
      Try.run(() -> callable.accept(in));
      return execute(in);
   }

   /**
    * @param request
    * @return
    */
   public A create(
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
   public OUT execute(final IN request) {
      try {
         return observe0(
            request,
            create()
         ).toBlocking().toFuture().get();
      } catch (Throwable e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * @param callback
    * @return
    */
   public Observable<OUT> observe(final Consumer<IN> callback) {
      final IN in = inProvider.get();
      if (callback != null) {
         callback.accept(in);
      }
      return observe(in);
   }

   /**
    * @param request
    * @return
    */
   public Observable<OUT> observe(
      final IN request) {
      return observe0(
         request,
         create()
      );
   }

   /**
    * @param request\
    */
   protected Observable<OUT> observe0(
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

      return
         Single.<OUT>create(
            subscriber -> {
               // Build observable.
               final Observable<OUT> observable = abstractAction.toObservable();
               final io.vertx.core.Context ctx = io.vertx.core.Vertx.currentContext();
               final ActionContext actionContext = abstractAction.actionContext();

               try {
                  observable.subscribe(
                     r -> {
                        if (subscriber.isUnsubscribed())
                           return;

                        if (ctx != null) {
                           ctx.runOnContext(a -> {
                              if (subscriber.isUnsubscribed())
                                 return;

                              AbstractAction.contextLocal.set(actionContext);
                              try {
                                 subscriber.onSuccess(r);
                              } finally {
                                 AbstractAction.contextLocal.remove();
                              }
                           });
                        } else {
                           AbstractAction.contextLocal.set(actionContext);
                           try {
                              subscriber.onSuccess(r);
                           } finally {
                              AbstractAction.contextLocal.remove();
                           }
                        }
                     },
                     e -> {
                        if (subscriber.isUnsubscribed())
                           return;

                        if (ctx != null) {
                           ctx.runOnContext(a -> {
                              if (subscriber.isUnsubscribed())
                                 return;

                              AbstractAction.contextLocal.set(actionContext);
                              try {
                                 subscriber.onError(e);
                              } finally {
                                 AbstractAction.contextLocal.remove();
                              }
                           });
                        } else {
                           AbstractAction.contextLocal.set(actionContext);
                           try {
                              subscriber.onError(e);
                           } finally {
                              AbstractAction.contextLocal.remove();
                           }
                        }
                     }
                  );
               } catch (Throwable e) {
//                        Try.run(() -> htx.close());
               }
            }
         ).toObservable();
   }
}
