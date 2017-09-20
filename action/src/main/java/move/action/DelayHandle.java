package move.action;

import kotlin.Unit;
import kotlin.coroutines.experimental.CoroutineContext;
import kotlinx.coroutines.experimental.CancellableContinuation;

/**
 *
 */
public class DelayHandle extends TimerHandle {

  CancellableContinuation<Unit> continuation;

  public DelayHandle(
      CancellableContinuation<Unit> continuation) {
    this.continuation = continuation;

    final CoroutineContext ctx = continuation.getContext();
    if (ctx instanceof HasTimers) {
      ((HasTimers) ctx).addTimer(this);
    }
  }

  void expired() {
    if (continuation == null) {
      unlink();
      continuation = null;
      return;
    }

    if (continuation.isCancelled() || continuation.isCompleted()) {
      unlink();
      continuation = null;
      return;
    }

    try {
      continuation.resume(Unit.INSTANCE);
    } catch (Throwable e) {
      // Ignore.
    } finally {
      unlink();
      continuation = null;
    }
  }

  @Override
  void unlink() {
    super.unlink();

    final CoroutineContext ctx = continuation.getContext();
    if (ctx instanceof HasTimers) {
      ((HasTimers) ctx).removeTimer(this);
    }
  }

  @Override
  void remove() {
    if (continuation == null) {
      return;
    }

    if (continuation.isCancelled() || continuation.isCompleted()) {
      unlink();
      continuation = null;
      return;
    }

    try {
      continuation.cancel(null);
    } catch (Throwable e) {
      // Ignore.
    } finally {
      unlink();
      continuation = null;
    }
  }

  @Override
  public void dispose() {
    remove();
  }
}
