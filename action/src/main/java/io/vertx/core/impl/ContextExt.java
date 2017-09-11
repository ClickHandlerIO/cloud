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
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public class ContextExt extends ContextImpl {

  // Sequence is a 24bit integer and epoch is a 40bit integer.
  public static final Logger log = LoggerFactory.getLogger(ContextExt.class);

  public final EventLoop eventLoop;

  public ContextExt(
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

  public void executeAsync(Handler<Void> task) {
    // No metrics, we are on the event loop.
    eventLoop.execute(wrapTask(task));
  }

  protected void setContextOnThread() {
    VertxThread current = (VertxThread) Thread.currentThread();
    current.setContext(this);
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
