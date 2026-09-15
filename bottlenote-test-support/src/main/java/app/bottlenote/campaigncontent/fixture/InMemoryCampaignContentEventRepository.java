package app.bottlenote.campaigncontent.fixture;

import app.bottlenote.campaigncontent.domain.CampaignContentEventLog;
import app.bottlenote.campaigncontent.domain.CampaignContentEventRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryCampaignContentEventRepository implements CampaignContentEventRepository {

  private final List<CampaignContentEventLog> events = new ArrayList<>();

  @Override
  public CampaignContentEventLog save(CampaignContentEventLog event) {
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
