package app.external.systemone.dto.response;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 질문 원시형별 응답. 확률과 신뢰도는 0~1 범위다. */
public sealed interface SystemOneAnswer {

  record Choice(String choice, Map<String, Double> probabilities, double confidence)
      implements SystemOneAnswer {
    public Choice {
      probabilities = copy(probabilities);
    }
  }

  /**
   * @param score 확률 가중 위치. 단계 사이 값일 수 있다
   * @param legend 단계 인덱스 → 단계 설명
   */
  record Score(
      double score,
      Map<String, String> legend,
      Map<String, Double> probabilities,
      double confidence)
      implements SystemOneAnswer {
    public Score {
      legend = copy(legend);
      probabilities = copy(probabilities);
    }
  }

  /** @param probability 예(1)일 확률. Noul은 신뢰도를 따로 주지 않는다 */
  record Noul(double probability) implements SystemOneAnswer {}

  // 공급자가 null 값을 보낼 수 있어 Map.copyOf 대신 순서를 보존하는 복사를 쓴다
  private static <V> Map<String, V> copy(Map<String, V> source) {
    return Collections.unmodifiableMap(new LinkedHashMap<>(source));
  }
}
