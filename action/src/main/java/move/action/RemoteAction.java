package move.action;


import static move.action.ActionConfig.DEFAULT_PARALLELISM;

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
public @interface RemoteAction {

  String method() default "POST";

  /**
   *
   * @return
   */
  int timeoutMillis() default 15_000;

  /**
   *
   * @return
   */
  int parallelism() default DEFAULT_PARALLELISM;

  /**
   *
   * @return
   */
  boolean guarded() default true;

  /**
   *
   * @return
   */
  String path() default "";
}
