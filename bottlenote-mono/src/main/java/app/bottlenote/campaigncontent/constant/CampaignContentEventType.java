package app.bottlenote.campaigncontent.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CampaignContentEventType {
  VIEW("페이지 첫 진입"),
  START("테스트 시작 또는 카드 뽑기 진입"),
  FINISH("마지막 문항·카드 선택 완료, 로그인 요구 직전"),
  RESULT("로그인 상태에서 결과 화면 노출");

  private final String description;

  // 결과 보기는 로그인 후에만 가능하므로 RESULT만 회원을 요구한다.
  public boolean requiresLogin() {
    return this == RESULT;
  }
}
