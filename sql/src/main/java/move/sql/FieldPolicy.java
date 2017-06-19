package move.sql;

/**
 *
 */
public @interface FieldPolicy {

  String fieldNames() default "";

  FieldConflictStrategy strategy();
}
