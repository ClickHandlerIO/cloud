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
     * @return
     */
    String[] primaryKey() default {};

    /**
     * @return
     */
    String[] shardKey() default {};

    /**
     *
     * @return
     */
    String[] columnStoreKey() default {};

    /**
     * Reference tables are
     * @return
     */
    boolean reference() default false;

    /**
     * @return
     */
    boolean columnStore() default false;
}
