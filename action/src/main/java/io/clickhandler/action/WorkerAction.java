package io.clickhandler.action;


import java.lang.annotation.*;

/**
 * A RemoteAction may be invoked by a system through a network connection.
 * <p/>
 * The Request/Response types must be serializable.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WorkerAction {
    String DEFAULT = "default";
    String BACKGROUND = "background";

    String queueName() default DEFAULT;
}
