package move.action;

import static move.action.ActionConfig.DEFAULT_PARALLELISM;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface InternalAction {
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
}
