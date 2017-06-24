package move.common;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 *
 */
public class Metrics {

  public static final String NAME = "app";

  public static MetricRegistry registry() {
    return SharedMetricRegistries.getOrCreate(NAME);
  }
}
