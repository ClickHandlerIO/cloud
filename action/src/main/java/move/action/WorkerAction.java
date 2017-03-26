package move.action;


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

    /**
     *
     * @return
     */
    String queueName() default DEFAULT;

    /**
     *
     * @return
     */
    boolean encrypted() default false;

    /**
     *
     * @return
     */
    boolean dedicated() default false;

   /**
    *
    * @return
    */
    String messageGroupId() default "";
}
