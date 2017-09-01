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

package io.vertx.core.impl;

import io.netty.channel.EventLoop;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import move.action.Action;
import move.action.ActionEventLoopGroup;
import move.action.LongSkipListMap;
import move.action.LongTreeMap;
import org.jetbrains.annotations.NotNull;

/**
 * Optimized EvenLoop Context for managing Action lifecycle. Action timeouts are efficiently tracked
 * at the trade-off of less precise expiration. Action timeouts happen in normalized 200ms blocks or
 * up to 5 times / second. Actions aren't intended to be completed in the nanoseconds, but rather
 * microsecond/ms/second.
 *
 * Actions are sequenced by the ceiling 200ms block of their calculated unix millisecond epoch
 * timeout. If an Action has no timeout then there is no associated timeout tracking cost. However,
 * every Action is locally tracked.
 *
 * @author Clay Molocznik
 */
public class ActionEventLoopContext extends ContextImpl {

  // Sequence is a 24bit integer and epoch is a 40bit integer.
  public static final long SEQUENCE_MASK = (1L << 24L) - 1L;
  public static final long FREQUENCY_ADJUSTMENT = 200L;

  private static final Logger log = LoggerFactory.getLogger(ActionEventLoopContext.class);

  public final EventLoop eventLoop;
  private final AtomicBoolean processingTimeouts = new AtomicBoolean(false);
  private final LongSkipListMap<Action> actionMap = new LongSkipListMap<>();
  private final LongSkipListMap<AtomicInteger> counterMap = new LongSkipListMap<>();
  private final LongSkipListMap<Action> nonTimeoutActionMap = new LongSkipListMap<>();
  private final AtomicLong nonTimeoutCounter = new AtomicLong(0L);

  public ActionEventLoopContext(
      @NotNull EventLoopContext eventLoopDefault,
      @NotNull VertxInternal vertxInternal,
      @NotNull JsonObject config,
      @NotNull EventLoop eventLoop) {
    super(vertxInternal, eventLoopDefault.internalBlockingPool, eventLoopDefault.workerPool, null,
        config, null, eventLoop);
    this.eventLoop = eventLoop;
  }

  public static EventLoop getEventLoop(ContextImpl context) {
    return context.nettyEventLoop();
  }

  public static void main(String[] args) throws InterruptedException {
    final Vertx vertx = Vertx.vertx();
    final ActionEventLoopGroup eventLoopGroup = ActionEventLoopGroup.Companion.get(vertx);

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
//      final long ceil = pack(System.currentTimeMillis() / FREQUENCY_ADJUSTMENT, 0);
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
//      long time = System.currentTimeMillis() / FREQUENCY_ADJUSTMENT;
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
    final long unix = System.currentTimeMillis() / FREQUENCY_ADJUSTMENT;
    AtomicInteger counter = timeoutMap.get(unix);
    if (counter == null) {
      counter = new AtomicInteger(0);
      timeoutMap.put(unix, counter);
    }
    return pack(unix, counter.incrementAndGet());
  }

  static long nextId(LongSkipListMap<AtomicInteger> timeoutMap) {
    final long unix = System.currentTimeMillis() / FREQUENCY_ADJUSTMENT;
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
    final long unix = epochMillis / FREQUENCY_ADJUSTMENT + FREQUENCY_ADJUSTMENT;
    final AtomicInteger counter = counterMap.computeIfAbsent(unix, (k) -> new AtomicInteger(0));
    return pack(unix, counter.incrementAndGet());
  }

  public long registerAction(Action action, long epochMillis) {
    final long id = nextId(epochMillis);
    actionMap.put(id, action);
    return id;
  }

  public long registerAction(Action action) {
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

  public void processTimeouts() {
    // Don't overload an already slammed EventLoop thread.
    if (!processingTimeouts.compareAndSet(false, true)) {
      return;
    }

    eventLoop.execute(() -> {
      try {
        // Give plenty of buffer so we don't race and duplicate IDs
        final long counterCeil =
            System.currentTimeMillis() / FREQUENCY_ADJUSTMENT - (FREQUENCY_ADJUSTMENT * 16);
        // Compute the ceiling timeout ID.
        final long timeoutCeil = pack(System.currentTimeMillis() / FREQUENCY_ADJUSTMENT, 0);

        // Look for timeouts.
        {
          LongSkipListMap.Node<AtomicInteger> entry = counterMap.removeFirstIfLessThan(counterCeil);
          while (entry != null) {
            // Next entry.
            entry = counterMap.removeFirstIfLessThan(counterCeil);
          }
        }
        {
          // Given we are sorted by lowest to highest with lowest being
          // the next Action that may have timed out.
          LongSkipListMap.Node<Action> entry = actionMap.removeFirstIfLessThan(timeoutCeil);
          while (entry != null) {
            // Cancel Action and flag as timed out.
            ((Action) entry.value).timedOut();

            // Next entry.
            entry = actionMap.removeFirstIfLessThan(timeoutCeil);
          }
        }
      } finally {
        processingTimeouts.compareAndSet(true, false);
      }
    });
  }

  public void executeAsync(Handler<Void> task) {
    // No metrics, we are on the event loop.
    eventLoop.execute(wrapTask(task));
  }

  protected Runnable wrapTask(Handler<Void> hTask) {
    return () -> {
      Thread th = Thread.currentThread();
      if (!(th instanceof VertxThread)) {
        throw new IllegalStateException(
            "Uh oh! Event loop eventLoop executing with wrong thread! Expected " + contextThread
                + " got " + th);
      }
      VertxThread current = (VertxThread) th;
      current.executeStart();
      current.setContext(this);
      try {
        hTask.handle(null);
      } catch (Throwable t) {
        log.error("Unhandled exception", t);
        Handler<Throwable> handler = exceptionHandler();
        if (handler == null) {
          handler = owner.exceptionHandler();
        }
        if (handler != null) {
          handler.handle(t);
        }
      } finally {
        // We don't unset the eventLoop after execution - this is done later when the eventLoop is closed via
        // VertxThreadFactory
        current.executeEnd();
      }
    };
  }

  @Override
  public boolean isEventLoopContext() {
    return true;
  }

  @Override
  public boolean isMultiThreadedWorkerContext() {
    return false;
  }

  @Override
  protected void checkCorrectThread() {
    // Skip checks.
//    Thread current = Thread.currentThread();
//    if (!(current instanceof VertxThread)) {
//      throw new IllegalStateException(
//          "Expected to be on Vert.x thread, but actually on: " + current);
//    } else if (contextThread != null && current != contextThread) {
//      throw new IllegalStateException(
//          "Event delivered on unexpected thread " + current + " expected: " + contextThread);
//    }
  }
}