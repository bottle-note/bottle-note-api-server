package app.external.systemone.dto.request;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** System One의 세 가지 질문 원시형. */
public sealed interface SystemOneQuestion {

  String instructions();

  /** 정의한 선택지 중 하나를 고른다. 선택지 키 → 설명. */
  record Choice(String instructions, Map<String, String> options) implements SystemOneQuestion {
    public static final int MIN_OPTIONS = 2;
    public static final int MAX_OPTIONS = 255;

    public Choice {
      requireInstructions(instructions);
      if (options == null || options.size() < MIN_OPTIONS || options.size() > MAX_OPTIONS) {
        throw new IllegalArgumentException(
            "choice 선택지는 %d~%d개여야 합니다".formatted(MIN_OPTIONS, MAX_OPTIONS));
      }
      options = Collections.unmodifiableMap(new LinkedHashMap<>(options));
    }
  }

  /** 순서가 있는 루브릭 단계로 평가한다. 첫 단계가 0이다. */
  record Score(String instructions, List<String> levels) implements SystemOneQuestion {
    public static final int MIN_LEVELS = 2;
    public static final int MAX_LEVELS = 10;

    public Score {
      requireInstructions(instructions);
      if (levels == null || levels.size() < MIN_LEVELS || levels.size() > MAX_LEVELS) {
        throw new IllegalArgumentException(
            "score 단계는 %d~%d개여야 합니다".formatted(MIN_LEVELS, MAX_LEVELS));
      }
      levels = List.copyOf(levels);
    }
  }

  /** 명제에 대한 예(1)/아니오(0) 확률을 구한다. */
  record Noul(String instructions) implements SystemOneQuestion {
    public Noul {
      requireInstructions(instructions);
    }
  }

  private static void requireInstructions(String instructions) {
    if (instructions == null || instructions.isBlank()) {
      throw new IllegalArgumentException("instructions는 비어 있을 수 없습니다");
    }
  }
}
