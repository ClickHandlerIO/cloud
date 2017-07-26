package move.action;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation describes the default command configuration when
 * executing an Action. This could be thought of as a "Hint" to
 * the ActionManager to better understand the intent of the Action.
 *
 * @author Clay Molocznik
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ActionConfig {

  int DEFAULT_PARALLELISM = 10000;
  int DEFAULT_TIMEOUT_MILLIS = 120_000;

  /**
   * @return
   */
  String groupKey() default "";

  /**
   * @return
   */
  String commandKey() default "";

  /**
   * @return
   */
  @Deprecated
  int maxExecutionMillis() default DEFAULT_TIMEOUT_MILLIS;

  /**
   * @return
   */
  @Deprecated
  int maxConcurrentRequests() default DEFAULT_PARALLELISM;

  /**
   *
   * @return
   */
  int timeoutMillis() default 120_000;

  /**
   *
   * @return
   */
  int parallelism() default DEFAULT_PARALLELISM;

  /**
   * @return
   */
  String description() default "";

  /**
   * @return
   */
  Class<? extends ActionProvider> provider() default ActionProvider.class;
}
