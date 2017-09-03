/*
 * Copyright (c) 2011-2016 The original author or authors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *      The Eclipse Public License is available at
 *      http://www.eclipse.org/legal/epl-v10.html
 *
 *      The Apache License v2.0 is available at
 *      http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package move.action

import io.vertx.circuitbreaker.CircuitBreakerOptions
import io.vertx.circuitbreaker.CircuitBreakerState
import io.vertx.core.Context
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.LongAdder
import java.util.function.Function

/**
 * Circuit Breaker pattern intended for mass concurrency.
 */
class ActionCircuitBreaker(private val name: String,
                           private val vertx: Vertx,
                           options: CircuitBreakerOptions?,
                           eventLoopGroup: ActionEventLoopGroup) {
   private val options: CircuitBreakerOptions
   private val periodicUpdateTask: Long
   internal val passed = AtomicInteger()
   private var openHandler: Handler<Void> = NOOP
   private var halfOpenHandler: Handler<Void> = NOOP
   private var closeHandler: Handler<Void> = NOOP
   private var fallback: Function<*, *>? = null
   private @Volatile var state = CircuitBreakerState.CLOSED
   private var checkFailuresTask: Long = -1L
//   private var failures: Long = 0

   private val failures = LongAdder()

   /**
    * For testing purpose only.
    *
    * @return retrieve the metrics.
    */
   val metrics: ActionCircuitBreakerMetrics

   init {
      Objects.requireNonNull(name)
      Objects.requireNonNull(vertx)

      if (options == null) {
         this.options = CircuitBreakerOptions()
      } else {
         this.options = CircuitBreakerOptions(options)
      }

      this.metrics = ActionCircuitBreakerMetrics(vertx, this, options)

      if (this.options.notificationPeriod > 0) {
         this.periodicUpdateTask = -1L
      } else {
         this.periodicUpdateTask = -1L
      }

      this.checkFailuresTask = vertx.setPeriodic(1000, { checkFailures() })
   }

   fun close(): ActionCircuitBreaker {
      if (this.periodicUpdateTask != -1L) {
         vertx.cancelTimer(this.periodicUpdateTask)
      }
      vertx.cancelTimer(this.checkFailuresTask)
      metrics.close()
      return this
   }

   @Synchronized
   fun openHandler(handler: Handler<Void>): ActionCircuitBreaker {
      Objects.requireNonNull(handler)
      openHandler = handler
      return this
   }

   @Synchronized
   fun halfOpenHandler(handler: Handler<Void>): ActionCircuitBreaker {
      Objects.requireNonNull(handler)
      halfOpenHandler = handler
      return this
   }

   @Synchronized
   fun closeHandler(handler: Handler<Void>): ActionCircuitBreaker {
      Objects.requireNonNull(handler)
      closeHandler = handler
      return this
   }

   fun <T> fallback(handler: Function<Throwable, T>): ActionCircuitBreaker {
      Objects.requireNonNull(handler)
      fallback = handler
      return this
   }

   //   @Synchronized
   fun reset(): ActionCircuitBreaker {
      failures.reset()

      if (state == CircuitBreakerState.CLOSED) {
         // Do nothing else.
         return this
      }

      state = CircuitBreakerState.CLOSED
      closeHandler.handle(null)
      return this
   }

   @Synchronized
   fun open(): ActionCircuitBreaker {
      if (state == CircuitBreakerState.OPEN) {
         return this
      }

      state = CircuitBreakerState.OPEN
      openHandler.handle(null)

      // Set up the attempt reset timer
      val period = options.resetTimeout
      if (period != -1L) {
         vertx.setTimer(period) { l -> attemptReset() }
      }

      return this
   }

   fun failureCount(): Long {
      return failures.sum()
   }

   fun state(): CircuitBreakerState {
      return state
   }

   private fun attemptReset(): ActionCircuitBreaker {
      if (state == CircuitBreakerState.OPEN) {
         passed.set(0)
         state = CircuitBreakerState.HALF_OPEN
         halfOpenHandler.handle(null)
      }
      return this
   }

   fun name(): String {
      return name
   }

   internal fun incrementFailures() {
      failures.increment()
   }

   private fun checkFailures() {
      if (failures.sum() >= options.maxFailures) {
         if (state != CircuitBreakerState.OPEN) {
            open()
         }
      }
   }

   fun options(): CircuitBreakerOptions {
      return options
   }

   companion object {
      private val NOOP: Handler<Void> = Handler { }
   }
}
