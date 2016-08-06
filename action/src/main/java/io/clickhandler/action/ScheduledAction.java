package io.clickhandler.action;

import java.lang.annotation.*;

/**
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ScheduledAction {
    /**
     * @return
     */
    int intervalSeconds() default 600;

    /**
     * @return
     */
    ScheduledActionType type() default ScheduledActionType.CLUSTER_SINGLETON;
}
