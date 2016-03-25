package io.clickhandler.action;

import java.lang.annotation.*;

/**
 * Internal Distributed Action for an instance of an Actor.
 * The Actor is stored on a single node in the cluster.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ActorAction {
    /**
     * @return
     */
    Class<? extends AbstractActor> actor() default AbstractActor.class;

    /**
     * @return
     */
    String name() default "";
}
