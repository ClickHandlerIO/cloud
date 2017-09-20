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
public @interface Daemon {
  /**
   * Name of Daemon.
   */
  String value() default "";

  /**
   * Role that applies to this Daemon.
   * The Daemon will not be started if the local node's
   * role does not match.
   *
   * @return
   */
  NodeRole role() default NodeRole.ALL;

  /**
   * Order in which the system should start each Daemon.
   * Daemons with the same "order" will then be sorted
   * by it's name or "value".
   *
   * @return
   */
  int order() default 0;
}
