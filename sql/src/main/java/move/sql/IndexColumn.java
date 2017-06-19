package move.sql;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface IndexColumn {

  String value();

  boolean asc() default true;
}
