package app.external.systemone.openai.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * OpenAI POST /v1/decisions 요청 본문.
 *
 * @param input 평가 대상. 문자열 또는 메시지 배열
 * @param questions 질문 배열. name은 응답에서 그대로 돌아온다
 */
public record OpenAiDecisionsRequest(String model, Object input, List<Question> questions) {

  /**
   * @param type predicate | choice | score
   * @param choices choice 전용 선택지
   * @param levels score 전용 단계. 낮은 단계부터 순서대로
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record Question(
      String type, String name, String instructions, List<Choice> choices, List<Level> levels) {

    public static Question predicate(String name, String instructions) {
      return new Question("predicate", name, instructions, null, null);
    }

    public static Question choice(String name, String instructions, List<Choice> choices) {
      return new Question("choice", name, instructions, choices, null);
    }

    public static Question score(String name, String instructions, List<Level> levels) {
      return new Question("score", name, instructions, null, levels);
    }
  }

  public record Choice(String value, String description) {}

  public record Level(String label, String description) {}
}
