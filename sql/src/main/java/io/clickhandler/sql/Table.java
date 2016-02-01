package io.clickhandler.sql;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a table mapping for a given class.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {

    /**
     * Name of the table mapped to the class.
     */
    String name() default "";

    /**
     * Determines whether a table needs to track the evolution of every record.
     * Another table will be created to record the evolutions.
     *
     * @return true if yes, false if no
     */
    boolean journal() default true;
}
