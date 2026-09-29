package app.bottlenote.statistics.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.time.LocalDateTime;

@DomainRepository
public interface MemberFunnelRepository {

  /** 방문은 텔레메트리, 가입은 users.create_at을 기간 조건으로 센다. 약관·첫 행동은 가입 회원의 현재 상태다. */
  MemberFunnelCounts countFunnel(LocalDateTime from, LocalDateTime toExclusive);
}
