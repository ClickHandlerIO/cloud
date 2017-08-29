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
 *
 * @author <a href="http://escoffier.me">Clement Escoffier</a>
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
  private LongAdder failures = new LongAdder();
  private LongAdder success = new LongAdder();
  private LongAdder timeout = new LongAdder();
  private LongAdder exceptions = new LongAdder();
//  private Histogram statistics = new ConcurrentHistogram(3);
  private Histogram statistics = new ActionHistogram(60000, 3);
  private Histogram cachedRolling1 = new ActionHistogram(60000, 3);
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
    final RollingWindow window = this.currentWindow;

    final long durationInMs = operation.durationInMs();
    this.durationMs.add(durationInMs);

    // Compute global statistics
    statistics.recordValue(operation.durationInMs());

    window.rollingOperationsDurationMs.add(durationInMs);
    window.rollingStatistic.recordValue(durationInMs);
    if (operation.exception) {
      exceptions.increment();
      window.rollingException.increment();
    } else if (operation.complete) {
      success.increment();
      window.rollingSuccess.increment();
    } else if (operation.timeout) {
      timeout.increment();
      window.rollingTimeout.increment();
    } else if (operation.failed) {
      failures.increment();
      window.rollingFailure.increment();
    }

    if (operation.fallbackSucceed) {
      window.rollingFallbackSuccess.increment();
    } else if (operation.fallbackFailed) {
      window.rollingFallbackFailure.increment();
    }

    if (operation.shortCircuited) {
      window.rollingShortCircuited.increment();
    }
  }

  public synchronized JsonObject toJson() {
    JsonObject json = new JsonObject();

    metricsCount.incrementAndGet();
    final RollingWindow window = this.currentWindow;
    final Histogram rollingStatistics = window.rollingStatistic.copy();
    this.currentWindow = new RollingWindow();
    final Histogram statistics = this.statistics.copy();
    final long end = System.currentTimeMillis();

    final long calls = statistics.getTotalCount();
    final long success = this.success.sum();
    final long failures = this.failures.sum();
    final long exceptions = this.exceptions.sum();
    final long timeout = this.timeout.sum();

    // Configuration
    json.put("begin", rollingStatistics.getStartTimeStamp());
    json.put("duration", (end - window.rollingStatistic.getEndTimeStamp()));
    json.put("resetTimeout", circuitBreakerResetTimeout);
    json.put("timeout", circuitBreakerTimeout);
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
    if (calls == 0) {
      json.put("totalSuccessPercentage", 0);
      json.put("totalErrorPercentage", 0);
    } else {
      json.put("totalSuccessPercentage", ((double) success / calls) * 100);
      json.put("totalErrorPercentage", ((double) (failures + exceptions + timeout) / calls) * 100);
    }

    addLatency(json, statistics, "total");

    final long rollingOperations = rollingStatistics.getTotalCount();
    final long rollingException = window.rollingException.sum();
    final long rollingFailure = window.rollingFailure.sum();
    final long rollingSuccess = window.rollingSuccess.sum();
    final long rollingTimeout = window.rollingTimeout.sum();
    final long rollingFallbackSuccess = window.rollingFallbackSuccess.sum();
    final long rollingFallbackFailure = window.rollingFallbackFailure.sum();
    final long rollingShortCircuited = window.rollingShortCircuited.sum();

    json.put("rollingOperationCount", rollingOperations - rollingShortCircuited);
    json.put("rollingErrorCount", rollingException + rollingFailure + rollingTimeout);
    json.put("rollingSuccessCount", rollingSuccess);
    json.put("rollingTimeoutCount", rollingTimeout);
    json.put("rollingExceptionCount", rollingException);
    json.put("rollingFailureCount", rollingFailure);
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

    Histogram rollingStatistic = nextRollingHistogram();
    LongAdder rollingOperationsDurationMs = new LongAdder();
    LongAdder rollingException = new LongAdder();
    LongAdder rollingFailure = new LongAdder();
    LongAdder rollingSuccess = new LongAdder();
    LongAdder rollingTimeout = new LongAdder();
    LongAdder rollingFallbackSuccess = new LongAdder();
    LongAdder rollingFallbackFailure = new LongAdder();
    LongAdder rollingShortCircuited = new LongAdder();
  }

  class Operation {

    final long constructed;
    private long begin;
    private volatile long end;
    private boolean complete;
    private boolean failed;
    private boolean timeout;
    private boolean exception;
    private boolean fallbackFailed;
    private boolean fallbackSucceed;
    private boolean shortCircuited;

    Operation() {
      constructed = System.nanoTime();
    }

    void begin() {
      begin = System.nanoTime();
    }

    void complete() {
      end = System.nanoTime();
      complete = true;
      ActionCircuitBreakerMetrics.this.complete(this);
    }

    void failed() {
      if (timeout || exception) {
        // Already completed.
        return;
      }
      end = System.nanoTime();
      failed = true;
      ActionCircuitBreakerMetrics.this.complete(this);
    }

    void timeout() {
      end = System.nanoTime();
      failed = false;
      timeout = true;
      ActionCircuitBreakerMetrics.this.complete(this);
    }

    void error() {
      end = System.nanoTime();
      failed = false;
      exception = true;
      ActionCircuitBreakerMetrics.this.complete(this);
    }

    void fallbackFailed() {
      fallbackFailed = true;
    }

    void fallbackSucceed() {
      fallbackSucceed = true;
    }

    void shortCircuited() {
      end = System.nanoTime();
      shortCircuited = true;
      ActionCircuitBreakerMetrics.this.complete(this);
    }

    long durationInMs() {
      return (end - begin) / 1000000;
    }
  }
}
