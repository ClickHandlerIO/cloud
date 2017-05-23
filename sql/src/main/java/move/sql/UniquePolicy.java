package move.sql;

/**
 *
 */
public @interface UniquePolicy {
    String name() default "";

    UniqueConflictStrategy strategy();
}
