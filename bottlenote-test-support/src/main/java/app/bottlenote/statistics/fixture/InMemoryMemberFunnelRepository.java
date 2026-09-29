package app.bottlenote.statistics.fixture;

import app.bottlenote.statistics.domain.MemberFunnelCounts;
import app.bottlenote.statistics.domain.MemberFunnelRepository;
import java.time.LocalDateTime;

/** 회원 퍼널 포트의 인메모리 구현. 테스트가 단계별 인원을 미리 넣는다. */
public class InMemoryMemberFunnelRepository implements MemberFunnelRepository {

  private MemberFunnelCounts counts = MemberFunnelCounts.empty();
  private LocalDateTime lastFrom;
  private LocalDateTime lastToExclusive;

  public void seed(MemberFunnelCounts counts) {
    this.counts = counts;
  }

  public LocalDateTime lastFrom() {
    return lastFrom;
  }

  public LocalDateTime lastToExclusive() {
    return lastToExclusive;
  }

  @Override
  public MemberFunnelCounts countFunnel(LocalDateTime from, LocalDateTime toExclusive) {
    this.lastFrom = from;
    this.lastToExclusive = toExclusive;
    return counts;
  }
}
