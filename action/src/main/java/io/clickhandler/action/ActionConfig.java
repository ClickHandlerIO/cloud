package io.clickhandler.action;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation describes the default command configuration when
 * executing an Action. This could be thought of as a "Hint" to
 * the ActionManager to better understand the intent of the Action.
 *
 * @author Clay Molocznik
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ActionConfig {
    /**
     * @return
     */
    String groupKey() default "";

    /**
     * @return
     */
    String commandKey() default "";

    /**
     * @return
     */
    String threadPoolKey() default "";

    /**
     * @return
     */
    int maxExecutionMillis() default 5000;

    /**
     *
     * @return
     */
    int maxConcurrentRequests() default 0;

    /**
     * @return
     */
    String description() default "";

    /**
     * @return
     */
    Class<? extends ActionProvider> provider() default ActionProvider.class;

    /**
     *
     * @return
     */
    ExecutionIsolationStrategy isolationStrategy() default ExecutionIsolationStrategy.SEMAPHORE;
}
