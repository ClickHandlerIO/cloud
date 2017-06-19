package move.sql;

/**
 *
 */
public enum FieldConflictStrategy {
  /**
   * Considering all regions, the most recent field update is kept.
   * The updates of other regions are discarded.
   * This is the default field resolution strategy.
   */
  LAST_WRITE_WINS,

  /**
   * Considering all regions, the least recent field update is kept.
   * The updates of other regions are discarded.
   */
  FIRST_WRITE_WINS,

  /**
   * The amounts added to the field in all regions (positive or negative) are summed to produce a
   * total.
   */
  DELTA,

  /**
   * The greatest value in any region is kept.
   */
  MAX,

  /**
   * The least value in any region is kept.
   */
  MIN,;
}
