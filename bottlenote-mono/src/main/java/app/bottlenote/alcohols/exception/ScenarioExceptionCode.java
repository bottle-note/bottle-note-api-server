package app.bottlenote.alcohols.exception;

import app.bottlenote.global.exception.custom.code.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ScenarioExceptionCode implements ExceptionCode {
  SCENARIO_NOT_FOUND(HttpStatus.NOT_FOUND, "그룹에 속한 시나리오 또는 버전을 찾을 수 없습니다."),
  SCENARIO_INVALID(HttpStatus.BAD_REQUEST, "시나리오 정의 또는 답변이 올바르지 않습니다."),
  SCENARIO_CONFLICT(HttpStatus.CONFLICT, "시나리오가 변경됐습니다. 최신 버전을 다시 조회해 주세요."),
  SCENARIO_SLOT_OCCUPIED(HttpStatus.CONFLICT, "그룹의 해당 슬롯은 이미 사용 중입니다."),
  SCENARIO_PUBLISHED_IMMUTABLE(
      HttpStatus.CONFLICT, "게시 이력이 있는 정의는 삭제하거나 수정할 수 없습니다. 새 초안을 만들거나 시나리오를 비활성화해 주세요."),
  SCENARIO_REFERENCE_IN_USE(HttpStatus.CONFLICT, "게시한 시나리오에서 참조하는 데이터의 의미는 변경할 수 없습니다.");
  private final HttpStatus httpStatus;
  private final String message;
}
