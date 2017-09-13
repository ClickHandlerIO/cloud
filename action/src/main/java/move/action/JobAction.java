package move.action;

import kotlinx.coroutines.experimental.JobSupport;
import move.action.MoveEventLoop.JobTimerHandle;

/**
 *
 */
public abstract class JobAction<IN, OUT>
    extends JobSupport
    implements Action<IN, OUT>, DeferredAction<OUT> {

  JobTimerHandle handle;

  public JobAction(boolean active) {
    super(active);
  }

  abstract void doTimeout();
}
