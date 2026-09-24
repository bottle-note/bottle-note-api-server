package app.external.systemone.dto.request;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * System One 평가 요청.
 *
 * @param state 평가 대상 상태. 문자열 또는 JSON으로 직렬화 가능한 객체
 * @param questions 질문 키별 질문. 응답은 같은 키로 돌아온다
 */
public record SystemOneRequest(Object state, Map<String, SystemOneQuestion> questions) {

  public SystemOneRequest {
    Objects.requireNonNull(state, "state는 필수입니다");
    if (questions == null || questions.isEmpty()) {
      throw new IllegalArgumentException("questions는 1개 이상이어야 합니다");
    }
    questions.forEach(
        (key, question) -> {
          if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("질문 키는 비어 있을 수 없습니다");
          }
          Objects.requireNonNull(question, "질문은 null일 수 없습니다: " + key);
        });
    questions = Collections.unmodifiableMap(new LinkedHashMap<>(questions));
  }

  public static SystemOneRequest of(Object state, String key, SystemOneQuestion question) {
    return new SystemOneRequest(state, Map.of(key, question));
  }
}
