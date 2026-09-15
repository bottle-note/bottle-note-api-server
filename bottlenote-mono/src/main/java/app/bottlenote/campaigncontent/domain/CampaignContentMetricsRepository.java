package app.bottlenote.campaigncontent.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

@DomainRepository
public interface CampaignContentMetricsRepository {

  /** 기간 내 이벤트를 제외 규칙을 적용해 순방문자·순회원 기준으로 센다. */
  CampaignContentEventCounts countEvents(
      Long campaignContentId,
      LocalDateTime from,
      LocalDateTime toExclusive,
      CampaignContentExclusion exclusion);

  /** 캠페인 콘텐츠별 기간 내 RESULT 순회원 수. 이벤트가 없으면 결과에서 빠진다. */
  Map<Long, Long> countResultMembers(
      Collection<Long> campaignContentIds,
      LocalDateTime from,
      LocalDateTime toExclusive,
      CampaignContentExclusion exclusion);
}
