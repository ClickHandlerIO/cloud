package io.clickhandler.sql.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Index {
    String name() default "";

    IndexColumn[] columns();

    boolean unique() default false;

    boolean clustered() default false;

    boolean journal() default false;

    String type() default "";
}
