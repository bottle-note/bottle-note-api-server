package app.external.systemone.jev.v1.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** Jev v1 POST /v1/systemone 요청 본문. */
public record JevV1Request(Object state, String model, Map<String, Question> questions) {

  /**
   * @param type choice | score | noul
   * @param criteria choice는 선택지 키→설명 객체, score는 단계 설명 배열, noul은 없음
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Question(String type, String instructions, Object criteria) {}
}
