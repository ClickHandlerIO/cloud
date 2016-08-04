package io.clickhandler.action;

/**
 *
 */
public @interface ScheduledAction {
    int delaySeconds() default 600;

    ScheduledActionType type() default ScheduledActionType.CLUSTER_SINGLETON;
}
