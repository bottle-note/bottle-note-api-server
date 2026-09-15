package app.bottlenote.campaigncontent.fixture;

import app.bottlenote.campaigncontent.domain.CampaignContentEventLog;
import app.bottlenote.campaigncontent.domain.CampaignContentEventRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryCampaignContentEventRepository implements CampaignContentEventRepository {

  private final List<CampaignContentEventLog> events = new ArrayList<>();
  private final InMemoryCampaignContentRepository campaignContentRepository;

  public InMemoryCampaignContentEventRepository(
      InMemoryCampaignContentRepository campaignContentRepository) {
    this.campaignContentRepository = campaignContentRepository;
  }

  @Override
  public CampaignContentEventLog register(CampaignContentEventLog event) {
    campaignContentRepository.recordEvent(event.getCampaignContentId());
    if (event.getId() == null) {
      ReflectionTestUtils.setField(event, "id", events.size() + 1L);
    }
    events.add(event);
    return event;
  }

  @Override
  public boolean existsByCampaignContentId(Long campaignContentId) {
    return events.stream().anyMatch(event -> event.getCampaignContentId().equals(campaignContentId));
  }

  public List<CampaignContentEventLog> events() {
    return List.copyOf(events);
  }
}
