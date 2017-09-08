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
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextExt;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import kotlin.Unit;
import kotlinx.coroutines.experimental.CancellableContinuation;
import org.jetbrains.annotations.NotNull;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.TriggerBuilder;

/**
 * Optimized EvenLoop Context for managing Action lifecycle. Action timeouts are efficiently tracked
 * at the trade-off of less precise expiration. Action timeouts happen in normalized 100ms blocks or
 * up to 5 times / second. Actions aren't intended to be completed in the nanoseconds
 * ('even though they can'), but rather microsecond/ms/second since network calls are usually made.
 *
 * Actions are sequenced by the ceiling 100ms block of their calculated unix millisecond epoch
 * deadline. If an Action has no deadline then there is no associated deadline tracking cost.
 * However, every Action is locally tracked.
 *
 * @author Clay Molocznik
 */
public class MoveEventLoop extends ContextExt {

  // Sequence is a 24bit integer and epoch is a 40bit integer.
  public static final long SEQUENCE_MASK = (1L << 24L) - 1L;
  public static final long TICK = 100L;

  public static final Logger log = LoggerFactory.getLogger(MoveEventLoop.class);

  public final EventLoop eventLoop;
  public final EventLoopDispatcher dispatcher = new EventLoopDispatcher(this);
  private final AtomicBoolean processingTick = new AtomicBoolean(false);
  private final LongSkipListMap<JobAction> actionMap = new LongSkipListMap<>();
  private final LongSkipListMap<AtomicInteger> counterMap = new LongSkipListMap<>();
  private final LongSkipListMap<Timer> timers = new LongSkipListMap<>();
  private final LongSkipListMap<JobAction> nonTimeoutActionMap = new LongSkipListMap<>();
  private final AtomicLong nonTimeoutCounter = new AtomicLong(0L);
  private final LongSkipListMap<JobAction> workerActions = new LongSkipListMap<>();
  private final HashMap<String, CronList> cronMap = new HashMap<>();
  private final Timer[] wheelTimers = new Timer[10000];
  private long tick = 0;
  private AtomicLong ticks = new AtomicLong(0L);

  public MoveEventLoop(
      @NotNull EventLoopContext eventLoopDefault,
      @NotNull VertxInternal vertxInternal,
      @NotNull JsonObject config,
      @NotNull EventLoop eventLoop) {
    super(eventLoopDefault, vertxInternal, config, eventLoop);
    this.eventLoop = eventLoop;
  }

  public static void main(String[] args) throws InterruptedException {
    for (int i = 0; i < 100; i++) {
      System.out.println(pack(i, i));
//      System.out.println(pack(1, 0));
    }


//    final CronTrigger trigger = TriggerBuilder
//        .newTrigger()
//        .withSchedule(CronScheduleBuilder.cronSchedule("0/25 0/5 * * * *"))
//        .forJob(JobBuilder.newJob().ofType(CronJob.class).build())
//        .startNow()
//        .build();

//    Thread.sleep(100000000);
  }

  private static void testPerf() {
    final Vertx vertx = Vertx.vertx();
    final MoveEventLoopGroup eventLoopGroup = MoveEventLoopGroup.Companion.get(vertx);

    System.out.println("EventLoop Threads = " + eventLoopGroup.getExecutors().size());
//    eventLoopGroup.getExecutors().forEach(t -> System.out.println(t.eventLoop.));

    LongSkipListMap<AtomicInteger> timeoutMap = new LongSkipListMap<>();
    LongSkipListMap<Long> actionMap = new LongSkipListMap<>();

//    BTreeMap<Long, AtomicInteger> timeoutMap = new BTreeMap<>();
//    BTreeMap<Long, Long> actionMap = new BTreeMap<>();
//
////    SkipList<Long, AtomicInteger> timeoutMap = new SkipList<>();
////    SkipList<Long, Long> actionMap = new SkipList<>();
//
//    LongTreeMap<Long> actionMap = new LongTreeMap<>();
//    LongTreeMap<AtomicInteger> timeoutMap = new LongTreeMap<>();
//
    for (int ii = 0; ii < 20; ii++) {
      actionMap.clear();
      long start = System.currentTimeMillis();
      long floor = nextId(timeoutMap);
      for (int i = 0; i < 1000000; i++) {
        long nextId = nextId(timeoutMap);
        actionMap.put(nextId, (long) i);
      }
      long ceil = nextId(timeoutMap);

      int size = actionMap.size();

      final long build = System.currentTimeMillis() - start;

      start = System.currentTimeMillis();
      for (int i = 0; i < 1000000; i++) {
//        actionMap.doRemoveFirstEntry(Long.MAX_VALUE);
//        actionMap.remove(actionMap.lastKey());
        actionMap.remove(ThreadLocalRandom.current().nextLong(floor, ceil));
//        actionMap.remove(actionMap.firstKey());
      }
      System.out
          .println("Construct: " + build + " -> Remove: " + (System.currentTimeMillis() - start));
      System.out.println("Actions Size: " + actionMap.size());
    }

    System.out.println("TimeOut Size: " + timeoutMap.size());
    System.out.println("Actions Size: " + actionMap.size());
//
//    for (; ; ) {
//      final long ceil = pack(System.currentTimeMillis() / TICK, 0);
//      System.out.println("Floor:");
//      System.out.println(ceil);
//      System.out.println("\t" + unpackTime(ceil) + " -> " + unpackSequence(ceil));
//      System.out.println();
//      for (int i = 0; i < 5; i++) {
//
//        final long id = nextId(timeoutMap);
//        System.out.println(id);
//        System.out.println("\t" + unpackTime(id) + " -> " + unpackSequence(id));
//      }
//      Thread.sleep(1000);
//
//      long time = System.currentTimeMillis() / TICK;
//      LongKeyEntry<Long> floor = actionMap.floorEntry(pack(time, 0));
//      LongKeyEntry<AtomicInteger> floorTimer = timeoutMap.floorEntry(pack(time, 0));
//
//      System.out.println(floor);
//      System.out.println(floorTimer);
//      if (floorTimer != null) {
//        System.out.println(floorTimer.getValue().get());
//      }
//    }
  }

  static long nextId(LongTreeMap<AtomicInteger> timeoutMap) {
    final long unix = System.currentTimeMillis() / TICK;
    AtomicInteger counter = timeoutMap.get(unix);
    if (counter == null) {
      counter = new AtomicInteger(0);
      timeoutMap.put(unix, counter);
    }
    return pack(unix, counter.incrementAndGet());
  }

  static long nextId(LongSkipListMap<AtomicInteger> timeoutMap) {
    final long unix = System.currentTimeMillis() / TICK;
    final AtomicInteger counter = timeoutMap.computeIfAbsent(unix, (k) -> new AtomicInteger(0));
    return pack(unix, counter.incrementAndGet());
  }

  /**
   *
   * @param unix
   * @param count
   * @return
   */
  static long pack(long unix, int count) {
    return ((unix & 0xffffffffffL) << 24) | (count & SEQUENCE_MASK);
  }

  /**
   *
   * @param packed
   * @return
   */
  static long unpackTime(long packed) {
    return packed >>> 24;
  }

  /**
   *
   * @param packed
   * @return
   */
  static int unpackSequence(long packed) {
    return (int) (packed & SEQUENCE_MASK);
  }

  /**
   *
   * @param epochMillis
   * @return
   */
  public long nextId(long epochMillis) {
    final long unix = epochMillis / TICK + TICK;
    final AtomicInteger counter = counterMap.computeIfAbsent(unix, (k) -> new AtomicInteger(0));
    return pack(unix, counter.incrementAndGet());
  }

  public long registerAction(JobAction action, long epochMillis) {
    final long id = nextId(epochMillis);
    actionMap.put(id, action);
    return id;
  }

  public long registerAction(JobAction action) {
    final long id = nonTimeoutCounter.incrementAndGet();
    nonTimeoutActionMap.put(id, action);
    return id;
  }

  public boolean removeTimeOutAction(long actionId) {
    return actionMap.remove(actionId) != null;
  }

  public boolean removeAction(long actionId) {
    return nonTimeoutActionMap.remove(actionId) != null;
  }

  TimerHandle registerTimer(CancellableContinuation<Unit> continuation, long delayMillis) {
    final long key = (System.currentTimeMillis() + delayMillis) / TICK;
    return new TimerHandle(
        timers.compute(key, k -> new Timer(continuation),
            v -> v.continuations.add(continuation)
        ), continuation);
  }

  /**
   *
   */
  void stop() {
    // Cancels all running Actions and Timers and Rejects new tasks.
  }

  void tick() {
    ticks.incrementAndGet();

    // Don't overload an already slammed EventLoop thread.
    if (!processingTick.compareAndSet(false, true)) {
      return;
    }

    eventLoop.execute(() -> {
      try {
        final long currentTick = ticks.get();

        final long tickDiff = currentTick - tick;
        if (tickDiff <= 0) {
          return;
        }

        final long now = System.currentTimeMillis() / TICK;

        // Give plenty of buffer so we don't race and duplicate IDs
        evictCounters(
            now - (TICK * 16)
        );
        // Compute the ceiling deadline ID.
        evictActions(pack(
            now, 0
        ));

//        for (long i = tick; i < currentTick; i++) {
//
//        }
        // Evict timers.
        evictTimers(now);

        // Update tick to the latest tick processed.
        tick = currentTick;
      } finally {
        processingTick.compareAndSet(true, false);
      }
    });
  }

  private void evictCounters(long counterCeil) {
    LongSkipListMap.Node<AtomicInteger> entry = counterMap.removeFirstIfLessThan(counterCeil);
    while (entry != null) {
      entry.value = null;

      // Next entry.
      entry = counterMap.removeFirstIfLessThan(counterCeil);
    }
  }

  private void evictActions(long timeoutCeil) {
    // Given we are sorted by lowest to highest with lowest being
    // the next Action that may have timed out.
    LongSkipListMap.Node<JobAction> entry = actionMap.removeFirstIfLessThan(timeoutCeil);
    while (entry != null) {
      // Cancel Action and flag as timed out.
      ((JobAction) entry.value).cancel(new ActionTimeoutException());
      entry.value = null;

      // Next entry.
      entry = actionMap.removeFirstIfLessThan(timeoutCeil);
    }
  }

  private void evictTimers(long ceil) {
    LongSkipListMap.Node<Timer> entry = timers.removeFirstIfLessThan(ceil);
    while (entry != null) {
      ((Timer) entry.value).close();
      entry.value = null;

      // Next entry.
      entry = timers.removeFirstIfLessThan(ceil);
    }
  }

  public void executeAsync(Handler<Void> task) {
    // No metrics, we are on the event loop.
    eventLoop.execute(wrapTask(task));
  }

  private static class CronJob implements Job {

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      System.out.println(jobExecutionContext.getFireTime());
    }
  }

  static class TimerHandle {

    private final Timer timer;
    private final CancellableContinuation<Unit> continuation;

    public TimerHandle(Timer timer,
        CancellableContinuation<Unit> continuation) {
      this.timer = timer;
      this.continuation = continuation;
    }

    public void remove() {
      timer.continuations.remove(continuation);
    }
  }

  private class WheelTimer {

    // Hour wheel.
    private WheelTimer[] wheel = new WheelTimer[(int) (1000 / TICK * 60 * 60)];
  }

  private class Timer {

    final List<CancellableContinuation> continuations;

    public Timer(CancellableContinuation<Unit> continuation) {
      continuations = new LinkedList<>();
      continuations.add(continuation);
    }

    public void close() {
      continuations.forEach(c -> {
        try {
          c.resume(Unit.INSTANCE);
        } catch (Throwable e) {
        }
      });
    }
  }

  private class CronList {

    CronTrigger trigger;
    CronJob job;
    String expression;
    HashSet<Daemon> daemons = new HashSet<>();
  }

  private class TimerList {

    public HashSet<Daemon> daemons = new HashSet<>();
  }
}
