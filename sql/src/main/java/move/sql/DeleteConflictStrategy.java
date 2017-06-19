package move.sql;

/**
 *
 */
public enum DeleteConflictStrategy {
  /**
   * The row that was updated in at least one region and deleted in at least one other region is
   * deleted. This is the default delete resolution strategy.
   */
  DELETE_WINS,

  /**
   * The row that was updated in at least one region and deleted in at least one other region is
   * kept.
   */
  DELETE_LOSES,;
}
