package app.external.systemone.dto.response;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** System One 호출 결과. 호출 측이 성공과 실패를 모두 명시적으로 처리하도록 sealed로 둔다. */
public sealed interface SystemOneResult {

  /**
   * @param model 공급자가 실제로 응답한 모델 버전 (예: jev-1.13.0)
   * @param answers 요청의 질문 키별 응답
   */
  record Success(String model, Map<String, SystemOneAnswer> answers, SystemOneUsage usage)
      implements SystemOneResult {
    public Success {
      answers = Collections.unmodifiableMap(new LinkedHashMap<>(answers));
    }

    public <T extends SystemOneAnswer> T answer(String key, Class<T> type) {
      SystemOneAnswer answer = answers.get(key);
      if (!type.isInstance(answer)) {
        throw new IllegalArgumentException("질문 키 %s의 응답이 %s가 아닙니다".formatted(key, type.getSimpleName()));
      }
      return type.cast(answer);
    }
  }

  /**
   * @param httpStatus 공급자 HTTP 상태. 응답을 받지 못했으면 비어 있다
   * @param message 원인 설명. 요청 본문과 인증 정보는 담지 않는다
   */
  record Failure(SystemOneFailureType type, Integer httpStatus, String message)
      implements SystemOneResult {

    public static Failure of(SystemOneFailureType type, String message) {
      return new Failure(type, null, message);
    }

    public Optional<Integer> status() {
      return Optional.ofNullable(httpStatus);
    }

    public boolean retryable() {
      return type.isRetryable();
    }
  }
}
