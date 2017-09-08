package move.action;

import io.reactivex.Single;
import kotlinx.coroutines.experimental.Deferred;
import kotlinx.coroutines.experimental.JobSupport;
import move.action.MoveEventLoop.JobTimerHandle;
import move.action.MoveEventLoop.JobDelayHandle;
import move.action.MoveEventLoop.TimerHandle;

/**
 *
 */
public abstract class JobAction<IN, OUT>
    extends JobSupport
    implements Action<IN, OUT>, Deferred<OUT> {

  JobTimerHandle handle;
  JobDelayHandle timer;

  public JobAction(boolean active) {
    super(active);
  }

  public abstract Single<OUT> asSingle();
}
