package app.bottlenote.global.timeseries;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record TimeSeriesPoint(String bucketAt, boolean partial, Map<String, Number> values) {

  public TimeSeriesPoint {
    // Map.of는 null 값을 담지 못하므로 순서 유지용 LinkedHashMap을 쓴다.
    Map<String, Number> copy = new LinkedHashMap<>();
    if (values != null) {
      copy.putAll(values);
    }
    values = Collections.unmodifiableMap(copy);
  }
}
