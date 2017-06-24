package move.remoting;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 *
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RemotingType {

}
