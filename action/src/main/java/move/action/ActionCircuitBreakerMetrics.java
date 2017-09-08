package move.action;

import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import org.HdrHistogram.ActionHistogram;
import org.HdrHistogram.Histogram;

/**
 * Circuit breaker metrics.
 */
public class ActionCircuitBreakerMetrics {

  private final long rollingWindow;
  private final ActionCircuitBreaker circuitBreaker;
  private final String node;

  private final long circuitBreakerResetTimeout;
  private final long circuitBreakerTimeout;
  private final long windowPeriodInNs;

  // Global statistics
  private final AtomicInteger metricsCount = new AtomicInteger(0);
  private LongAdder durationMs = new LongAdder();
  private LongAdder cpuTime = new LongAdder();
  private LongAdder blocking = new LongAdder();
  private LongAdder failures = new LongAdder();
  private LongAdder success = new LongAdder();
  private LongAdder timeout = new LongAdder();
  private LongAdder exceptions = new LongAdder();
  //  private Histogram statistics = new ConcurrentHistogram(3);
//  private Histogram statistics = new ConcurrentHistogram(3);
  private Histogram statistics = new ActionHistogram(60000, 3);
  //  private Histogram cachedRolling1 = new ConcurrentHistogram(3);
  private Histogram cachedRolling1 = new ActionHistogram(60000, 3);
  //  private Histogram cachedRolling2 = new ConcurrentHistogram(3);
  private Histogram cachedRolling2 = new ActionHistogram(60000, 3);
  private volatile RollingWindow currentWindow = new RollingWindow();

  ActionCircuitBreakerMetrics(Vertx vertx, ActionCircuitBreaker circuitBreaker,
      CircuitBreakerOptions options) {
    this.circuitBreaker = circuitBreaker;
    this.circuitBreakerTimeout = circuitBreaker.options().getTimeout();
    this.circuitBreakerResetTimeout = circuitBreaker.options().getResetTimeout();
    this.rollingWindow = options.getMetricsRollingWindow();
    this.node =
        vertx.isClustered() ? ((VertxInternal) vertx).getClusterManager().getNodeID() : "local";
    this.windowPeriodInNs = rollingWindow * 1000000;
  }

  public void close() {
    // do nothing by default.
  }

  Operation enqueue() {
    return new Operation();
  }

  public void complete(Operation operation) {
//    final RollingWindow window = this.currentWindow;

//    final long durationInMs = operation.durationInNanos();
//    this.durationMs.add(durationInMs);

    // Compute global statistics
//    statistics.recordValue(operation.durationInNanos());

//    cpuTime.add(operation.cpu);
//    window.cpuTime.add(operation.cpu);

//    if (operation.blocking > 0L) {
//      blocking.add(operation.blocking);
////      window.blocking.add(operation.blocking);
//    }

//    window.operationDurationMs.add(durationInMs);
//    window.stats.recordValue(durationInMs);
//    if (operation.exception) {
//      exceptions.increment();
////      window.exception.increment();
//    } else if (operation.complete) {
//      success.increment();
////      window.success.increment();
//    } else if (operation.timeout) {
//      timeout.increment();
////      window.timeout.increment();
//    } else if (operation.failed) {
//      failures.increment();
////      window.failure.increment();
//    }
//
//    if (operation.fallbackSucceed) {
////      window.fallbackSuccess.increment();
//    } else if (operation.fallbackFailed) {
////      window.fallbackFailure.increment();
//    }
//
//    if (operation.shortCircuited) {
////      window.shortCircuited.increment();
//    }
  }

  public synchronized JsonObject toJson() {
    JsonObject json = new JsonObject();

    metricsCount.incrementAndGet();
    final RollingWindow window = this.currentWindow;
    final Histogram rollingStatistics = window.stats.copy();
    this.currentWindow = new RollingWindow();
    final Histogram statistics = this.statistics.copy();
    final long end = System.currentTimeMillis();

    final long calls = statistics.getTotalCount();
    final long success = this.success.sum();
    final long failures = this.failures.sum();
    final long exceptions = this.exceptions.sum();
    final long timeout = this.timeout.sum();
    final long cpu = this.cpuTime.sum();
    final long blocking = this.blocking.sum();

    // Configuration
    json.put("begin", rollingStatistics.getStartTimeStamp());
    json.put("duration", (end - rollingStatistics.getEndTimeStamp()));
    json.put("resetTimeout", circuitBreakerResetTimeout);
    json.put("deadline", circuitBreakerTimeout);
    json.put("metricRollingWindow", rollingWindow);
    json.put("name", circuitBreaker.name());
    json.put("node", node);

    // Current state
    json.put("state", circuitBreaker.state());
    json.put("failures", circuitBreaker.failureCount());

    // Global metrics
    json.put("totalErrorCount", failures + exceptions + timeout);
    json.put("totalSuccessCount", success);
    json.put("totalTimeoutCount", timeout);
    json.put("totalExceptionCount", exceptions);
    json.put("totalFailureCount", failures);
    json.put("totalOperationCount", calls);
    json.put("totalCpu", cpu);
    json.put("totalBlocking", blocking);
    if (calls == 0) {
      json.put("totalSuccessPercentage", 0);
      json.put("totalErrorPercentage", 0);
    } else {
      json.put("totalSuccessPercentage", ((double) success / calls) * 100);
      json.put("totalErrorPercentage", ((double) (failures + exceptions + timeout) / calls) * 100);
    }

    addLatency(json, statistics, "total");

    final long rollingOperations = rollingStatistics.getTotalCount();
    final long rollingException = window.exception.sum();
    final long rollingFailure = window.failure.sum();
    final long rollingSuccess = window.success.sum();
    final long rollingTimeout = window.timeout.sum();
    final long rollingCPU = window.cpuTime.sum();
    final long rollingFallbackSuccess = window.fallbackSuccess.sum();
    final long rollingFallbackFailure = window.fallbackFailure.sum();
    final long rollingShortCircuited = window.shortCircuited.sum();
    final long rollingBlocking = window.blocking.sum();

    json.put("rollingOperationCount", rollingOperations - rollingShortCircuited);
    json.put("rollingErrorCount", rollingException + rollingFailure + rollingTimeout);
    json.put("rollingSuccessCount", rollingSuccess);
    json.put("rollingTimeoutCount", rollingTimeout);
    json.put("rollingExceptionCount", rollingException);
    json.put("rollingFailureCount", rollingFailure);
    json.put("rollingCpu", rollingCPU);
    json.put("rollingBlocking", rollingBlocking);
    if (rollingOperations == 0) {
      json.put("rollingSuccessPercentage", 0);
      json.put("rollingErrorPercentage", 0);
    } else {
      json.put("rollingSuccessPercentage", ((double) rollingSuccess / rollingOperations) * 100);
      json.put("rollingErrorPercentage",
          ((double) (rollingException + rollingFailure + rollingTimeout + rollingShortCircuited)
              / rollingOperations) * 100);
    }

    json.put("rollingFallbackSuccessCount", rollingFallbackSuccess);
    json.put("rollingFallbackFailureCount", rollingFallbackFailure);
    json.put("rollingShortCircuitedCount", rollingShortCircuited);

    addLatency(json, rollingStatistics, "rolling");
    return json;
  }

  private void addLatency(JsonObject json, Histogram histogram, String prefix) {
    json.put(prefix + "LatencyMean", histogram.getMean());
    json.put(prefix + "Latency", new JsonObject()
        .put("0", histogram.getValueAtPercentile(0))
        .put("25", histogram.getValueAtPercentile(25))
        .put("50", histogram.getValueAtPercentile(50))
        .put("75", histogram.getValueAtPercentile(75))
        .put("90", histogram.getValueAtPercentile(90))
        .put("95", histogram.getValueAtPercentile(95))
        .put("99", histogram.getValueAtPercentile(99))
        .put("99.5", histogram.getValueAtPercentile(99.5))
        .put("100", histogram.getValueAtPercentile(100)));
  }

  private Histogram nextRollingHistogram() {
    final Histogram histogram = metricsCount.get() % 2 == 0 ?
        cachedRolling1 : cachedRolling2;

    histogram.reset();

    return histogram;
  }

  private class RollingWindow {

    Histogram stats = nextRollingHistogram();
    LongAdder operationDurationMs = new LongAdder();
    LongAdder cpuTime = new LongAdder();
    LongAdder blocking = new LongAdder();
    LongAdder exception = new LongAdder();
    LongAdder failure = new LongAdder();
    LongAdder success = new LongAdder();
    LongAdder timeout = new LongAdder();
    LongAdder fallbackSuccess = new LongAdder();
    LongAdder fallbackFailure = new LongAdder();
    LongAdder shortCircuited = new LongAdder();
  }

  class Operation {

    long begin;
    long cpu;
    long cpuBegin;
    long blocking;
    long blockingBegin;
    long child;
    long childCpu;
    long childBlocking;
//    boolean complete;
//    boolean failed;
//    boolean timeout;
//    boolean exception;
//    boolean fallbackFailed;
//    boolean fallbackSucceed;
//    boolean shortCircuited;

    Operation() {
    }

    void begin() {
      begin = System.nanoTime();
    }

    void cpuStart() {
      cpuBegin = System.nanoTime();
    }

    long cpuEnd() {
      long increaseBy = System.nanoTime() - cpuBegin;
      cpu += increaseBy;
      ActionCircuitBreakerMetrics.this.cpuTime.add(increaseBy);
      return increaseBy;
    }

    synchronized void blockingBegin() {
      blockingBegin = System.nanoTime();
    }

    synchronized void blockingEnd() {
      blocking += System.nanoTime() - blockingBegin;
      ActionCircuitBreakerMetrics.this.blocking.add(blocking);
    }

    void complete() {
      ActionCircuitBreakerMetrics.this.success.increment();
      final long duration = (System.nanoTime() - begin) / 1000000;
      ActionCircuitBreakerMetrics.this.durationMs.add(duration);
      cachedRolling1.recordValue(duration);
//      cachedRolling1.recordValue(duration);
    }

    void failed() {
      failures.increment();
      final long duration = (System.nanoTime() - begin) / 1000000;
      ActionCircuitBreakerMetrics.this.durationMs.add(duration);
    }

    void timeout() {
      ActionCircuitBreakerMetrics.this.timeout.increment();
      final long duration = (System.nanoTime() - begin) / 1000000;
      ActionCircuitBreakerMetrics.this.durationMs.add(duration);
    }

    void error() {
      ActionCircuitBreakerMetrics.this.exceptions.increment();
      final long duration = (System.nanoTime() - begin) / 1000000;
      ActionCircuitBreakerMetrics.this.durationMs.add(duration);
    }

    void fallbackFailed() {

    }

    void fallbackSucceed() {

    }

    void shortCircuited() {
      final long duration = (System.nanoTime() - begin) / 1000000;
      ActionCircuitBreakerMetrics.this.durationMs.add(duration);
    }
  }
}
