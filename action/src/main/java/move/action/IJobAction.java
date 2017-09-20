package move.action;

import kotlinx.coroutines.experimental.JobSupport;

/**
 *
 */
public abstract class IJobAction<IN, OUT>
    extends JobSupport
    implements Action<IN, OUT>, DeferredAction<OUT> {

  JobTimerHandle handle;

  public IJobAction(boolean active) {
    super(active);
  }

  abstract void doTimeout();
}
