package io.clickhandler.action;

import java.lang.annotation.*;

/**
 * Internal Distributed Action for an instance of a Actor.
 * The Actor is stored on a single node in the cluster.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ActorAction {
    /**
     * @return
     */
    Class actor() default Object.class;

    /**
     * @return
     */
    String name() default "";
}
