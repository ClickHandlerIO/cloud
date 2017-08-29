package move.action;


import static move.action.ActionConfig.DEFAULT_CONCURRENCY;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A RemoteAction may be invoked by a system through a network connection.
 * <p/>
 * The Request/Response types must be serializable.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WorkerAction {

  /**
   * @return
   */
  boolean encrypted() default false;

  /**
   * @return
   */
  boolean fifo() default false;

  /**
   *
   * @return
   */
  int concurrency() default DEFAULT_CONCURRENCY;

  /**
   *
   * @return
   */
  String queueName() default "";
}
