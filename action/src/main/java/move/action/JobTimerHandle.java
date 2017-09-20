package move.action;

/**
 *
 */
public class JobTimerHandle extends TimerHandle {

  IJobAction action;

  public JobTimerHandle(IJobAction action) {
    this.action = action;
  }

  void expired() {
    if (action == null) {
      return;
    }

    try {
      if (action.isActive()) {
        action.doTimeout();
      }
    } finally {
      unlink();
      action = null;
    }
  }

  void remove() {
    if (action == null) {
      return;
    }

    unlink();

    // Maybe make it easier for GC.
    action = null;
  }

  @Override
  public void dispose() {
    remove();
  }
}
