package io.clickhandler.action;

import java.lang.annotation.*;

/**
 * Internal Distributed Action for an instance of a Store.
 * The Store is stored on a single node in the cluster.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface StoreAction {
    /**
     * @return
     */
    Class store() default Object.class;

    /**
     * @return
     */
    String name() default "";
}
