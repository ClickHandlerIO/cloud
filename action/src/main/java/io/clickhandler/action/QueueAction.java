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
     * Base name of the queue.
     * For scalability if, a number may be appended to this name.
     *
     * @return
     */
    String name() default "";

    /**
     * Determines whether reliability is required.
     * This will tell the kernel to pick the most
     * reliable backing possible.
     * <p>
     * Usually this means a network Queue such as Redis or AmazonSQS
     *
     * @return
     */
    boolean reliable() default true;
}
