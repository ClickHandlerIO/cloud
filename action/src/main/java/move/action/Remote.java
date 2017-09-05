package move.action;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A Remote may be invoked by a system through a network connection.
 * <p/>
 * The Request/Response types must be serializable.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Remote {
  String value() default "";

  /**
   * Default Time in Milliseconds the Action is allowed to be in "ACTIVE" state.
   */
  int timeout() default 15_000;

  /**
   *
   * @return
   */
  boolean guarded() default true;

  /**
   * HTTP Method
   */
  String method() default "POST";

  /**
   *
   * @return
   */
  boolean websocket() default true;

  /**
   *
   * @return
   */
  String path() default "";

  /**
   *
   * @return
   */
  ActionVisibility visibility() default ActionVisibility.PUBLIC;

}
