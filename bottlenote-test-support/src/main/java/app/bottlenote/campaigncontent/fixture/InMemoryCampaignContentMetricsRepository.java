package app.bottlenote.campaigncontent.fixture;

import app.bottlenote.campaigncontent.domain.CampaignContentEventCounts;
import app.bottlenote.campaigncontent.domain.CampaignContentExclusion;
import app.bottlenote.campaigncontent.domain.CampaignContentMetricsRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** 집계 SQL은 통합 테스트가 검증하고, 여기서는 테스트가 넣은 집계 결과를 돌려준다. */
public class InMemoryCampaignContentMetricsRepository implements CampaignContentMetricsRepository {

  private final Map<Long, CampaignContentEventCounts> counts = new HashMap<>();
  private final Map<Long, Long> resultMembers = new HashMap<>();
  private LocalDateTime lastFrom;
  private LocalDateTime lastToExclusive;
  private CampaignContentExclusion lastExclusion;

  public void seedCounts(Long campaignContentId, CampaignContentEventCounts eventCounts) {
    counts.put(campaignContentId, eventCounts);
  }

  public void seedResultMembers(Long campaignContentId, long members) {
    resultMembers.put(campaignContentId, members);
  }

  public LocalDateTime lastFrom() {
    return lastFrom;
  }

  public LocalDateTime lastToExclusive() {
    return lastToExclusive;
  }

  public CampaignContentExclusion lastExclusion() {
    return lastExclusion;
  }

  @Override
  public CampaignContentEventCounts countEvents(
      Long campaignContentId,
      LocalDateTime from,
      LocalDateTime toExclusive,
      CampaignContentExclusion exclusion) {
    remember(from, toExclusive, exclusion);
    return counts.getOrDefault(campaignContentId, CampaignContentEventCounts.empty());
  }

  @Override
  public Map<Long, Long> countResultMembers(
      Collection<Long> campaignContentIds,
      LocalDateTime from,
      LocalDateTime toExclusive,
      CampaignContentExclusion exclusion) {
    remember(from, toExclusive, exclusion);
    Map<Long, Long> result = new HashMap<>();
    for (Long campaignContentId : campaignContentIds) {
      if (resultMembers.containsKey(campaignContentId)) {
        result.put(campaignContentId, resultMembers.get(campaignContentId));
      }
    }
    return result;
  }

  private void remember(
      LocalDateTime from, LocalDateTime toExclusive, CampaignContentExclusion exclusion) {
    this.lastFrom = from;
    this.lastToExclusive = toExclusive;
    this.lastExclusion = exclusion;
  }
}
