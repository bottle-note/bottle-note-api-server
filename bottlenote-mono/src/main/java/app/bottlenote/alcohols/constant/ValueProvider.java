package app.bottlenote.alcohols.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ValueProvider implements CodeLabel {
  ADMIN("관리자", "admin", "관리자가 어드민에서 직접 입력하거나 조절한다"),
  AI("AI", "ai", "AI가 리뷰·설명 같은 텍스트에서 추출해 제안한다"),
  AUTOMATIC("자동 계산", "automatic", "배치나 적재 작업이 규칙대로 계산해 채운다"),
  ANY("제한 없음", "any", "관리자, AI, 자동 계산 어느 쪽이든 넣을 수 있다");

  private final String korName;
  private final String engName;
  private final String description;
}
