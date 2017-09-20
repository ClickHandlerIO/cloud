package move.action;

import kotlinx.coroutines.experimental.DisposableHandle;

/**
 *
 */
public class TimerHandle implements DisposableHandle {

  TimerHandle prev;
  TimerHandle next;

  void expired() {
    // NOOP
  }

  void remove() {
    // NOOP
  }

  @Override
  public void dispose() {

  }

  @Override
  public void unregister() {
    dispose();
  }

  void unlink() {
    final TimerHandle p = prev;
    if (p == null) {
      return;
    }

    final TimerHandle n = next;
    p.next = n;
    if (n != null) {
      n.prev = p;
    }

    prev = null;
    next = null;
  }
}
