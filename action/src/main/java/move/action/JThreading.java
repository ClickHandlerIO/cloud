package move.action;

import static net.openhft.affinity.AffinityStrategies.ANY;
import static net.openhft.affinity.AffinityStrategies.DIFFERENT_CORE;
import static net.openhft.affinity.AffinityStrategies.DIFFERENT_SOCKET;
import static net.openhft.affinity.AffinityStrategies.SAME_CORE;
import static net.openhft.affinity.AffinityStrategies.SAME_SOCKET;

import net.openhft.affinity.Affinity;
import net.openhft.affinity.AffinityLock;
import net.openhft.affinity.AffinityStrategies;

/**
 *
 */
public class JThreading {

  public static void main(String... args) throws InterruptedException {
    AffinityLock al = AffinityLock.acquireLock();
    System.out.println(Affinity.isJNAAvailable());
    try {
      // find a cpu on a different socket, otherwise a different core.
      AffinityLock readerLock = al.acquireLock(DIFFERENT_SOCKET, DIFFERENT_CORE);
      new Thread(new SleepRunnable(readerLock, false), "reader").start();

      // find a cpu on the same core, or the same socket, or any free cpu.
      AffinityLock writerLock = readerLock.acquireLock(SAME_CORE, SAME_SOCKET, ANY);
      new Thread(new SleepRunnable(writerLock, false), "writer").start();

      Thread.sleep(200);
    } finally {
      al.release();
    }

    // allocate a whole core to the engine so it doesn't have to compete for resources.
    al = AffinityLock.acquireCore(false);
    new Thread(new SleepRunnable(al, true), "engine").start();

    Thread.sleep(200);
    System.out.println("\nThe assignment of CPUs is\n" + AffinityLock.dumpLocks());
  }

  static class SleepRunnable implements Runnable {

    private final AffinityLock affinityLock;
    private final boolean wholeCore;

    SleepRunnable(AffinityLock affinityLock, boolean wholeCore) {
      this.affinityLock = affinityLock;
      this.wholeCore = wholeCore;
    }

    public void run() {
      affinityLock.bind(wholeCore);
      try {
        int cpuId = affinityLock.cpuId();
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        affinityLock.release();
      }
    }
  }
}
