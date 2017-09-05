package move.action;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A Remote may be invoked by a system through a network connection.
 * <p/>
 * The Request/Reply types must be serializable.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Worker {

  /**
   * Action's name.
   *
   * Default name is the FQN of the class minus if it
   * starts with "action."
   *
   * e.g.
   *    inventory.CreateStock
   *
   * @return
   */
  String value() default "";

  /**
   * @return
   */
  boolean fifo() default false;

  /**
   * Default Time in Milliseconds the Action is allowed to
   * be in "ACTIVE" state.
   *
   * @return
   */
  int timeout() default 30_000;

  /**
   *
   * @return
   */
  int concurrency() default 32;

  /**
   *
   * @return
   */
  String queueName() default "";
}
