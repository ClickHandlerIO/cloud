package move.action;

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
@Target(ElementType.TYPE)
public @interface ScheduledAction {

  /**
   * @return
   */
  int intervalSeconds() default 600;

  /**
   * @return
   */
  ScheduledActionType type() default ScheduledActionType.CLUSTER_SINGLETON;
}
