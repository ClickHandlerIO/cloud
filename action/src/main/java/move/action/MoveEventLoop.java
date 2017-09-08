/*
 * Copyright (c) 2011-2013 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package move.action;

import io.netty.channel.EventLoop;
import io.vertx.core.Handler;
import io.vertx.core.impl.ContextExt;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import kotlin.Unit;
import kotlinx.coroutines.experimental.CancellableContinuation;
import org.jetbrains.annotations.NotNull;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Optimized EvenLoop Context for managing Action lifecycle. Action timeouts are efficiently tracked
 * at the trade-off of less precise expiration. Action timeouts happen in normalized 100ms blocks or
 * up to 5 times / second. Actions aren't intended to be completed in the nanoseconds ('even though
 * they can'), but rather microsecond/ms/second since network calls are usually made.
 *
 * Actions are sequenced by the ceiling 100ms block of their calculated unix millisecond epoch
 * deadline. If an Action has no deadline then there is no associated deadline tracking cost.
 * However, every Action is locally tracked.
 *
 * @author Clay Molocznik
 */
public class MoveEventLoop extends ContextExt {

  // Tick duration in milliseconds.
  public static final long TICK = 100L;
  public static final int WHEEL = (int) (TimeUnit.MINUTES.toMillis(10) / TICK);

  public static final Logger log = LoggerFactory.getLogger(MoveEventLoop.class);

  // Netty EventLoop.
  public final EventLoop eventLoop;
  // Move kotlinx-coroutines Dispatcher.
  public final MoveDispatcher dispatcher = new MoveDispatcher(this);
  // Processing tick flag.
  // This is atomic since it's used outside the context of the EventLoop thread.
  private final AtomicBoolean processingTick = new AtomicBoolean(false);
  // Timer map for Jobs that won't fit on the wheel.
  private final LongSkipListMap<Timer> timers = new LongSkipListMap<>();
  // Wheel timer related.
  private final Timer[] timerWheel = new Timer[WHEEL];

  private long tick = 0;
  private int tickIndex = 0;
  private long wheelThreshold;
  private AtomicLong ticks = new AtomicLong(0L);

  private Timer nonTimedTimmer = new Timer();

  public MoveEventLoop(
      @NotNull EventLoopContext eventLoopDefault,
      @NotNull VertxInternal vertxInternal,
      @NotNull JsonObject config,
      @NotNull EventLoop eventLoop) {
    super(eventLoopDefault, vertxInternal, config, eventLoop);
    this.eventLoop = eventLoop;
    this.tickIndex = tickIndex(System.currentTimeMillis());

    initWheel();
  }

  public static void main(String[] args) throws InterruptedException {
    for (int tick = 0; tick < 100; tick++) {
      int tickIndex = (int) ((System.currentTimeMillis() / TICK) % WHEEL);
      System.out.println(tickIndex);
//      System.out.println(pack(1, 0));
      Thread.sleep(100);
    }
  }

  private void initWheel() {
    for (int i = 0; i < timerWheel.length; i++) {
      timerWheel[i] = new Timer();
    }
  }

  private int tickIndex(long epoch) {
    return tick == 0 ? 0 : (int) ((epoch / tick) % WHEEL);
  }


  JobTimerHandle registerJob(JobAction action) {
    final JobTimerHandle handle = new JobTimerHandle(action);
    nonTimedTimmer.add(handle);
    return handle;
  }

  JobTimerHandle registerJobTimer(JobAction action, long epochMillis) {
    final JobTimerHandle handle = new JobTimerHandle(action);

    if (epochMillis < wheelThreshold) {
      timerWheel[tickIndex].add(handle);
    } else {
      final long key = epochMillis / TICK;
      Timer timer = timers.get(key);
      if (timer == null) {
        timer = new Timer(handle);
        timers.put(key, timer);
      } else {
        timer.add(handle);
      }
    }

    return handle;
  }

  JobDelayHandle createDelayTimer(JobAction deferred, CancellableContinuation<Unit> continuation,
      long delayMillis) {
    final long key = (System.currentTimeMillis() + delayMillis) / TICK;

    final JobDelayHandle handle = new JobDelayHandle(deferred, continuation);

    Timer timer = timers.get(key);
    if (timer == null) {
      timer = new Timer(handle);
      timers.put(key, timer);
    } else {
      timer.add(handle);
    }

    return handle;
  }

  /**
   *
   */
  void stop() {
    // Cancels all running Actions and Timers and Rejects new tasks.
    eventLoop.execute(() -> {
      processFullWheel();
      nonTimedTimmer.expired();
    });
  }

  void tick() {
    ticks.incrementAndGet();

    // Don't overload an already slammed EventLoop thread.
    if (!processingTick.compareAndSet(false, true)) {
      return;
    }

    eventLoop.execute(() -> {
      try {
        final long lastTick = this.tick;
        final long currentTick = ticks.get();

        final long now = System.currentTimeMillis();
        wheelThreshold = System.currentTimeMillis() + (TICK * WHEEL);

        int newTickIndex = tickIndex(now);

        final long tickDiff = currentTick - lastTick;

        if (tickDiff > timerWheel.length || tickDiff < 0) {
          // This is a serious problem.
          // EventLoop thread was likely blocked.
          // Or the memory is corrupted.
          log.error("Extreme Tick lag [" + tickDiff + "] which is " +
              (TICK * tickDiff) + "ms. EventLoop thread blocked!!!");
          processFullWheel();
        } else {
          processWheel(newTickIndex + 1);
        }

        this.tickIndex = newTickIndex;

        // Evict timers.
        evictTimers(now / TICK);

        // Update tick to the latest tick processed.
        tick = currentTick;
      } finally {
        processingTick.compareAndSet(true, false);
      }
    });
  }

  private void processFullWheel() {
    for (int tick = 0; tick < timerWheel.length; tick++) {
      timerWheel[tick].expired();
    }
  }

  private void processWheel(int toIndex) {
    if (tickIndex < toIndex) {
      for (int tick = tickIndex; tick < toIndex; tick++) {
        timerWheel[tick].expired();
      }
    } else if (tickIndex > toIndex) {
      for (int tick = tickIndex; tick < timerWheel.length; tick++) {
        timerWheel[tick].expired();
      }
      for (int tick = 0; tick < toIndex; tick++) {
        timerWheel[tick].expired();
      }
    }
  }

  private void evictTimers(long ceil) {
    Timer timer = timers.pollFirstEntryIfLessThan(ceil);
    while (timer != null) {
      timer.expired();
      timer = timers.pollFirstEntryIfLessThan(ceil);
    }
  }

  public void executeAsync(Handler<Void> task) {
    // No metrics, we are on the event loop.
    eventLoop.execute(wrapTask(task));
  }

  public interface ITimerHandle {

    void timerHandleExpired();

    void timerHandleRemove();

    void timerHandleUnlink();
  }

  private static class CronJob implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      System.out.println(jobExecutionContext.getFireTime());
    }
  }

  /**
   *
   */
  static class TimerHandle {

    TimerHandle prev;
    TimerHandle next;

    void expired() {
      // NOOP
    }

    void remove() {
      // NOOP
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

  /**
   *
   */
  static class JobTimerHandle extends TimerHandle {

    JobAction action;

    public JobTimerHandle(JobAction action) {
      this.action = action;
    }

    void expired() {
      if (action == null) {
        return;
      }

      try {
        if (action.isActive()) {
          action.cancel(new TimeoutException());
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
  }

  /**
   *
   */
  static class JobDelayHandle extends TimerHandle {

    JobAction action;
    CancellableContinuation<Unit> continuation;

    public JobDelayHandle(
        JobAction action,
        CancellableContinuation<Unit> continuation) {
      this.action = action;
      this.continuation = continuation;
    }

    void expired() {
      if (continuation == null) {
        action = null;
        continuation = null;
        return;
      }

      if (continuation.isCancelled() || continuation.isCompleted()) {
        action = null;
        continuation = null;
        return;
      }

      try {
        if (action == null || action.isCancelled() || action.isCompleted()) {
          continuation.cancel(null);
        } else {
          continuation.resume(Unit.INSTANCE);
        }
      } catch (Throwable e) {
        // Ignore.
      } finally {
        unlink();
        action = null;
        continuation = null;
      }
    }

    @Override
    void remove() {
      if (continuation == null) {
        return;
      }

      if (continuation.isCompleted() || continuation.isCompleted()) {
        unlink();
        action = null;
        continuation = null;
        return;
      }

      try {
        continuation.cancel(null);
      } catch (Throwable e) {
        // Ignore.
      } finally {
        unlink();
        action = null;
        continuation = null;
      }
    }
  }

  /**
   * Singly linked list of TimerHandles.
   */
  private class Timer {

    final TimerHandle head = new TimerHandle();
    int count;

    public Timer() {
      count = 0;
    }

    public Timer(TimerHandle handle) {
      count = 1;
      head.next = handle;
      handle.prev = head;
    }

    void add(TimerHandle handle) {
      final TimerHandle n = head.next;
      handle.next = n;
      head.next = handle;
      handle.prev = head;
      if (n != null) {
        n.prev = handle;
      }
    }

    void expired() {
      for (TimerHandle x = head.next; x != null; x = x.next) {
        x.expired();
      }
    }
  }


  /**
   *
   */
  private class CronList {

    CronTrigger trigger;
    CronJob job;
    String expression;
    HashSet<Daemon> daemons = new HashSet<>();
  }
}
