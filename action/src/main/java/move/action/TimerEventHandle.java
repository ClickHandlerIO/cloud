package move.action;

/**
 *
 */
public class TimerEventHandle extends TimerHandle {

  int type;
  HasTimers hasTimers;

  public TimerEventHandle(int type, HasTimers hasTimers) {
    this.hasTimers = hasTimers;
    hasTimers.addTimer(this);
  }

  void expired() {
    if (hasTimers == null) {
      unlink();
      return;
    }

    try {
      hasTimers.onTimer(this);
    } catch (Throwable e) {
      // Ignore.
    } finally {
      unlink();
    }
  }

  @Override
  void unlink() {
    super.unlink();

    if (hasTimers != null) {
      hasTimers.removeTimer(this);
      hasTimers = null;
    }
  }

  @Override
  void remove() {
    if (hasTimers == null) {
      return;
    }

    unlink();
  }

  @Override
  public void dispose() {
    remove();
  }
}
