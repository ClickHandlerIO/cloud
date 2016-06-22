package io.clickhandler.action;

import java.lang.annotation.*;

/**
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface QueueAction {
    /**
     * Base actorName of the queue.
     * For scalability if, a number may be appended to this actorName.
     *
     * @return
     */
    String name() default "";
}
