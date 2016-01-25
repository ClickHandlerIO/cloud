package io.clickhandler.action;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ActionConfig {
    String groupKey() default "";

    String commandKey() default "";

    String threadPoolKey() default "";

    boolean sessionRequired() default true;

    int maxExecutionMillis() default 0;

    Class provider() default Object.class;

    String description() default "";
}
