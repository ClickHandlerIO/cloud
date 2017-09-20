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
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.impl.ContextExt;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.impl.VertxThread;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlinx.coroutines.experimental.CancellableContinuation;
import org.jetbrains.annotations.NotNull;

/**
 * Optimized EvenLoop Context for managing Action lifecycle. Action timeouts are efficiently tracked
 * at the trade-off of less precise expiration. Action timeouts happen in normalized 200ms blocks or
 * up to 5 times / second. If more precise timeouts are needed, then use LockSupport.
 *
 * Every MoveEventLoop processes network I/O and user tasks in the same thread. Network I/O is
 * handled by "epoll" if on Linux. "kqueue" if on BSD. "NIO" is the fallback. Native transport is
 * preferable especially in regards to garbage collection since most buffers point to native memory.
 * This is ideal for Move's goal of "real-time" or very low latency and is preferred over maximum
 * throughput.
 *
 * Actions are sequenced by the ceiling 100ms block of their calculated unix millisecond epoch
 * deadline. If an Action has no deadline then there is no associated deadline tracking cost.
 * However, every Action has a strong reference stored on the MoveEventLoop to ensure it's not
 * collected by the GC.
 *
 * An entire Action's lifecycle happens within a single MoveEventLoop.
 *
 * @author Clay Molocznik
 */
public class MoveEventLoop extends ContextExt {

  // Tick duration in milliseconds.
  public static final long TICK_MS = 200L;
  // Wheel size. 10 minutes is a good amount of time.
  // It degrades to a SkipList that's unbounded for timeouts greater than wheel size.
  public static final int WHEEL_LENGTH = (int) (TimeUnit.MINUTES.toMillis(10) / TICK_MS);
  public static final int MAX_TICKS_BEHIND = (int) (TimeUnit
      .SECONDS
      .toMillis(
          MoveAppKt.getMODE() == Mode.DEV
              ? 1000
              : MoveAppKt.getMODE() == Mode.TEST
                  ? 1000
                  : 10)
      / TICK_MS);
  public static final Logger log = LoggerFactory.getLogger(MoveEventLoop.class);
  static final ThreadLocal<MoveEventLoop> THREAD_LOCAL = new ThreadLocal<>();
  // MoveApp kotlinx-coroutines Dispatcher.
  public final MoveDispatcher dispatcher = new MoveDispatcher(this);
  // Netty EventLoop.
  final EventLoop eventLoop;
  // Processing tick flag.
  // This is atomic since it's used outside the context of the EventLoop thread.
  private final AtomicBoolean processingTick = new AtomicBoolean(false);
  // Timer map for Jobs that won't fit on the wheel.
  private final LongSkipListMap<Timer> timers = new LongSkipListMap<>();
  // Wheel timer related.
  private final Timer[] timerWheel = new Timer[WHEEL_LENGTH];
  private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
  long epoch;
  long epochTick;
  JobAction<?, ?> job;
  private VertxThread thread;
  private long id;
  private long tick = 0;
  private int wheelIndex = 0;
  private long maxWheelEpoch;
  private long maxWheelEpochTick;
  private AtomicLong ticks = new AtomicLong(0L);
  private Timer unlimitedTimer = new Timer();

  private Object actorStore;
  private Object counterStore;
  private Object bus;

  private AtomicLong cpuTime = new AtomicLong(0L);

  public MoveEventLoop(
      @NotNull EventLoopContext eventLoopDefault,
      @NotNull VertxInternal vertxInternal,
      @NotNull JsonObject config,
      @NotNull EventLoop eventLoop) {
    super(eventLoopDefault, vertxInternal, config, eventLoop);
    this.eventLoop = eventLoop;
    this.wheelIndex = wheelIndexFromEpoch(System.currentTimeMillis());

    initWheel();

    epoch = System.currentTimeMillis();
    epochTick = epoch / TICK_MS;
    maxWheelEpochTick = epochTick + WHEEL_LENGTH - 1;
  }

  public static void main(String[] args) throws InterruptedException {
    for (int tick = 0; tick < 100; tick++) {
      int tickIndex = (int) ((System.currentTimeMillis() / TICK_MS) % WHEEL_LENGTH);
      System.out.println(tickIndex);
//      System.out.println(pack(1, 0));
      Thread.sleep(100);
    }
  }

  public static long toTick(long epoch) {
    return epoch / TICK_MS;
  }

  public static long calculateTicks(long time, TimeUnit unit) {
    return countTicks(unit.toMillis(time));
  }

  public static long countTicks(long millis) {
    if (millis < 1L) {
      return 0L;
    }
    final long ticks = millis / TICK_MS;
    return ticks < 1L ? 0L : ticks;
  }

  void init(Function0<Unit> block) {
    try {
      // Init thread.
      nettyEventLoop().submit(() -> {
        setContextOnThread();
        thread = (VertxThread) Thread.currentThread();
        id = thread.getId();
        block.invoke();
      }).await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public long getId() {
    return id;
  }

  public long getEpoch() {
    return epoch;
  }

  public long getEpochTick() {
    return epochTick;
  }

  public long getTick() {
    return tick;
  }

  public int getWheelIndex() {
    return wheelIndex;
  }

  public long getMaxWheelEpoch() {
    return maxWheelEpoch;
  }

  public long getMaxWheelEpochTick() {
    return maxWheelEpochTick;
  }

  public JobAction<?, ?> getJob() {
    return job;
  }

  private void initWheel() {
    for (int i = 0; i < timerWheel.length; i++) {
      timerWheel[i] = new Timer();
    }
  }

  /**
   * Calculate tick index for an epoch.
   */
  int wheelIndexFromEpoch(long epoch) {
    return tick == 0 ? 0 : (int) ((epoch / TICK_MS) % WHEEL_LENGTH);
  }

  /**
   * Calculate tick index for an epoch.
   */
  int wheelIndexFromEpochTick(long epochTick) {
    final int index = (int) ((epochTick) % WHEEL_LENGTH);
    return index;
  }

  long tickFor(long epoch) {
    if (epoch < 1L || epoch > maxWheelEpoch) {
      return -1L;
    }

    return epoch / TICK_MS;
  }

  /**
   *
   * @param action
   * @return
   */
  JobTimerHandle registerJob(IJobAction action) {
    final JobTimerHandle handle = new JobTimerHandle(action);
    unlimitedTimer.add(handle);
    return handle;
  }

  /**
   *
   * @param action
   * @param tickEpoch
   * @return
   */
  JobTimerHandle registerJobTimerForTick(IJobAction action, long tickEpoch) {
    if (tickEpoch < epochTick) {
      return null;
    }

    final JobTimerHandle handle = new JobTimerHandle(action);

    if (tickEpoch < maxWheelEpochTick) {
      timerWheel[wheelIndexFromEpochTick(tickEpoch)].add(handle);
    } else {
      Timer timer = timers.get(tickEpoch);
      if (timer == null) {
        timer = new Timer(handle);
        timers.put(tickEpoch, timer);
      } else {
        timer.add(handle);
      }
    }

    return handle;
  }

  /**
   *
   * @param action
   * @param epochMillis
   * @return
   */
  JobTimerHandle registerJobTimer(IJobAction action, long epochMillis) {
    if (epochMillis < epoch) {
      return null;
    }

    final JobTimerHandle handle = new JobTimerHandle(action);

    if (epochMillis < maxWheelEpoch) {
      timerWheel[wheelIndexFromEpoch(epochMillis)].add(handle);
    } else {
      final long key = epochMillis / TICK_MS;
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

  /**
   *
   * @param continuation
   * @param delayMillis
   * @return
   */
  DelayHandle scheduleDelay(
      CancellableContinuation<Unit> continuation,
      long delayMillis) {

    final long epochMillis = System.currentTimeMillis() + delayMillis;
    final DelayHandle handle = new DelayHandle(continuation);

    if (epochMillis < maxWheelEpoch) {
      timerWheel[wheelIndexFromEpoch(epochMillis)].add(handle);
    } else {
      final long key = epochMillis / TICK_MS;
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

  /**
   *
   * @param hasTimers
   * @param delayMillis
   * @return
   */
  TimerEventHandle scheduleTimer(
      int type,
      HasTimers hasTimers,
      long delayMillis) {

    final long epochMillis = System.currentTimeMillis() + delayMillis;
    final TimerEventHandle handle = new TimerEventHandle(type, hasTimers);

    if (epochMillis < maxWheelEpoch) {
      timerWheel[wheelIndexFromEpoch(epochMillis)].add(handle);
    } else {
      final long key = epochMillis / TICK_MS;
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

  long toTicks(long millis) {
    if (millis < TICK_MS) {
      return 1;
    }
    return (long) Math.ceil((double) millis / (double) TICK_MS);
  }

  /**
   *
   */
  void stop() {
    // Cancels all running Actions and Timers and Rejects new tasks.
    eventLoop.execute(() -> {
      processFullWheel();
      unlimitedTimer.expired();
    });
  }

  void tick() {
    ticks.incrementAndGet();

    // Don't overload an already slammed EventLoop thread.
    if (!processingTick.compareAndSet(false, true)) {
      final long tickDiff = ticks.get() - tick;

      if (tickDiff > MAX_TICKS_BEHIND || tickDiff < 0) {
        // This is a serious problem.
        // EventLoop thread was likely blocked.
        // Or the memory is corrupted.
        log.error("Extreme Tick lag [" + tickDiff + "] which is " +
            (TICK_MS * tickDiff) + "ms. EventLoop thread blocked!!!");
      }

      return;
    }

    eventLoop.execute(() -> {
      try {
        final long lastTick = this.tick;
        final long currentTick = ticks.get();
        final long now = System.currentTimeMillis();
        final long currentEpochTick = now / TICK_MS;
        final int newTickIndex = wheelIndexFromEpoch(now);
        final long ticksToProcess = currentTick - lastTick;

        try {
          // Process wheel.
          if (ticksToProcess > timerWheel.length || ticksToProcess < 0) {
            // This is a serious problem.
            // EventLoop thread was likely blocked.
            // Or the memory is corrupted.
            log.error("Extreme Tick lag [" + ticksToProcess + "] which is " +
                (TICK_MS * ticksToProcess) + "ms. EventLoop thread blocked!!!");
            processFullWheel();
          } else {
            processWheel(newTickIndex + 1);
          }

          // Evict timers.
          evictTimers(currentEpochTick);
        } finally {
          // Update tick to the latest tick processed.
          tick = currentTick;
          wheelIndex = newTickIndex;
          // Update min Epoch
          epoch = now;
          // Update epochTick
          epochTick = currentEpochTick;
          // Calculate new max wheel epoch tick.
          maxWheelEpochTick = epochTick + WHEEL_LENGTH - 1;
          maxWheelEpoch = maxWheelEpochTick * TICK_MS;
        }
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
    if (wheelIndex < toIndex) {
      for (int tick = wheelIndex; tick < toIndex; tick++) {
        timerWheel[tick].expired();
      }
    } else if (wheelIndex > toIndex) {
      for (int tick = wheelIndex; tick < timerWheel.length; tick++) {
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

  public void execute(Function0<Unit> block) {
    eventLoop.execute(block::invoke);
  }

  public void execute(Runnable runnable) {
    eventLoop.execute(runnable);
  }

  public void executeAsync(Handler<Void> task) {
    // No metrics, we are on the event loop.
    eventLoop.execute(wrapTask(task));
  }

  public <T> void executeBlocking0(Handler<Future<T>> blockingCodeHandler,
      Handler<AsyncResult<T>> asyncResultHandler) {
    super.executeBlocking(blockingCodeHandler, asyncResultHandler);
  }

  /**
   * Doubly linked list of TimerHandles.
   */
  private class Timer extends TimerHandle {

    final TimerHandle head = this;

    public Timer() {
    }

    public Timer(TimerHandle handle) {
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
      TimerHandle x = next;
      while (x != null) {
        x.expired();
        x = next;
      }
    }
  }
}
