package app.bottlenote.campaigncontent.fixture;

import app.bottlenote.campaigncontent.domain.CampaignContent;
import app.bottlenote.campaigncontent.domain.CampaignContentRepository;
import app.bottlenote.campaigncontent.exception.CampaignContentException;
import app.bottlenote.campaigncontent.exception.CampaignContentExceptionCode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryCampaignContentRepository implements CampaignContentRepository {

  private final Map<Long, CampaignContent> database = new LinkedHashMap<>();
  private final Set<Long> contentIdsWithEvents = new HashSet<>();
  private long sequence = 0L;

  @Override
  public CampaignContent register(CampaignContent campaignContent) {
    if (existsByCode(campaignContent.getCode())) {
      throw new CampaignContentException(
          CampaignContentExceptionCode.CAMPAIGN_CONTENT_DUPLICATE_CODE);
    }
    Long id = campaignContent.getId();
    if (id == null) {
      id = ++sequence;
      ReflectionTestUtils.setField(campaignContent, "id", id);
    }
    database.put(id, campaignContent);
    return campaignContent;
  }

  @Override
  public Optional<CampaignContent> findById(Long id) {
    return Optional.ofNullable(database.get(id));
  }

  @Override
  public Optional<CampaignContent> findByCode(String code) {
    return database.values().stream().filter(content -> content.getCode().equals(code)).findFirst();
  }

  @Override
  public boolean existsByCode(String code) {
    return findByCode(code).isPresent();
  }

  @Override
  public void remove(CampaignContent campaignContent) {
    if (contentIdsWithEvents.contains(campaignContent.getId())) {
      throw new CampaignContentException(CampaignContentExceptionCode.CAMPAIGN_CONTENT_HAS_EVENTS);
    }
    database.remove(campaignContent.getId());
  }

  void recordEvent(Long campaignContentId) {
    if (!database.containsKey(campaignContentId)) {
      throw new CampaignContentException(CampaignContentExceptionCode.CAMPAIGN_CONTENT_NOT_FOUND);
    }
    contentIdsWithEvents.add(campaignContentId);
  }

  @Override
  public List<CampaignContent> searchForAdmin(
      String keyword, Boolean isActive, int page, int size) {
    return filtered(keyword, isActive)
        .sorted(Comparator.comparing(CampaignContent::getId).reversed())
        .skip((long) page * size)
        .limit(size)
        .toList();
  }

  @Override
  public long countForAdmin(String keyword, Boolean isActive) {
    return filtered(keyword, isActive).count();
  }

  private Stream<CampaignContent> filtered(String keyword, Boolean isActive) {
    return database.values().stream()
        .filter(content -> keyword == null || matchesKeyword(content, keyword))
        .filter(content -> isActive == null || isActive.equals(content.getIsActive()));
  }

  private static boolean matchesKeyword(CampaignContent content, String keyword) {
    String lowered = keyword.toLowerCase(Locale.ROOT);
    return content.getName().toLowerCase(Locale.ROOT).contains(lowered)
        || content.getCode().contains(lowered);
  }
}
