package app.bottlenote.statistics.facade;

import app.bottlenote.statistics.dto.request.MemberCohortRetentionRequest;
import app.bottlenote.statistics.dto.request.MemberFunnelRequest;
import app.bottlenote.statistics.dto.response.MemberCohortRetentionResponse;
import app.bottlenote.statistics.dto.response.MemberFunnelResponse;

public interface MemberStatisticsFacade {

  /** 방문 → 가입 → 필수 약관 동의 → 첫 행동 단계별 인원과 전환율. 구간 상한은 텔레메트리 보존과 같은 90일이다. */
  MemberFunnelResponse findFunnel(MemberFunnelRequest request);

  /** 가입 버킷별로 offset번째 버킷에 다시 활동한 비율. 활동 원천은 user_daily_activities 롤업이다. */
  MemberCohortRetentionResponse findCohortRetention(MemberCohortRetentionRequest request);
}
