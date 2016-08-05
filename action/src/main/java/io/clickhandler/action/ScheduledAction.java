package io.clickhandler.action;

import java.lang.annotation.*;

/**
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ScheduledAction {
    int intervalSeconds() default 600;

    ScheduledActionType type() default ScheduledActionType.CLUSTER_SINGLETON;
}
