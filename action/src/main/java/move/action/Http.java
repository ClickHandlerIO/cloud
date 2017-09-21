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
public @interface Http {

  int DEFAULT_TIMEOUT = 30_000;

  /**
   * Path expression.
   */
  String path() default "";

  /**
   * Default Time in Milliseconds the Action is allowed to be in "ACTIVE" state. HttpResponse will
   * automatically be closed.
   */
  int timeout() default DEFAULT_TIMEOUT;

  /**
   * HTTP Method
   */
  Method method() default Method.ALL;

  /**
   *
   * @return
   */
  String[] consumes() default {};

  /**
   *
   * @return
   */
  String[] produces() default {};

  /**
   *
   * @return
   */
  boolean websocket() default true;

  /**
   *
   * @return
   */
  ActionVisibility visibility() default ActionVisibility.PUBLIC;

  enum Method {
    ALL,
    OPTIONS,
    GET,
    HEAD,
    POST,
    PUT,
    DELETE,
    TRACE,
    CONNECT,
    PATCH,
  }
}
