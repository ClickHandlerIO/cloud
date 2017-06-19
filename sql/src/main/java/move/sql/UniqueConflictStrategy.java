package move.sql;

/**
 *
 */
public enum UniqueConflictStrategy {
  /**
   * Considering all regions, the latest row updated is kept. Others rows are discarded.
   */
  LAST_WRITE_WINS,

  /**
   * Considering all regions, the earliest row updated is kept. Others rows are discarded.
   */
  FIRST_WRITE_WINS,

  /**
   * A new row is created consisting of the identical value(s) in the unique field(s) and
   * the conflict resolutions for all other fields. Note that this runs the [ strategy ]
   * for all other fields.
   */
  MERGE,

  /**
   * Generate a new value for the unique field and keep every conflicting row. Note that
   * this requires the unique constraint to be on a single field and that field to be a sequence.
   */
  REINCREMENT,;
}
