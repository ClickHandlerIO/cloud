package move.action;

import io.reactivex.Single;
import kotlinx.coroutines.experimental.Deferred;
import kotlinx.coroutines.experimental.JobSupport;

/**
 *
 */
public abstract class JobAction<IN, OUT>
    extends JobSupport
    implements Action<IN, OUT>, Deferred<OUT> {

  public JobAction(boolean active) {
    super(active);
  }

  public abstract Single<OUT> asSingle();
}
