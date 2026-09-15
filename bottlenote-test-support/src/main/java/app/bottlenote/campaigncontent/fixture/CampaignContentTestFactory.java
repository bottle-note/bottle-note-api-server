package app.bottlenote.campaigncontent.fixture;

import app.bottlenote.campaigncontent.constant.CampaignContentEventType;
import app.bottlenote.campaigncontent.domain.CampaignContent;
import app.bottlenote.campaigncontent.domain.CampaignContentEventLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

/** 캠페인 콘텐츠와 참여 이벤트를 영속화하는 테스트 팩토리. */
@Component
public class CampaignContentTestFactory {

  @PersistenceContext private EntityManager em;

  @Transactional
  @NotNull
  public CampaignContent persistCampaignContent(@NotNull String code, @NotNull String name) {
    return persistCampaignContent(code, name, true);
  }

  @Transactional
  @NotNull
  public CampaignContent persistCampaignContent(
      @NotNull String code, @NotNull String name, @NotNull Boolean isActive) {
    CampaignContent campaignContent =
        CampaignContent.builder()
            .code(code)
            .name(name)
            .description(name + " 설명")
            .isActive(isActive)
            .build();
    em.persist(campaignContent);
    em.flush();
    return campaignContent;
  }

  @Transactional
  @NotNull
  public CampaignContentEventLog persistEvent(
      @NotNull Long campaignContentId,
      @NotNull CampaignContentEventType eventType,
      @Nullable String visitorId,
      @Nullable Long userId,
      @Nullable String ipAddress,
      @NotNull LocalDateTime occurredAt) {
    return persistEvent(
        campaignContentId, eventType, visitorId, userId, ipAddress, "모바일", occurredAt);
  }

  @Transactional
  @NotNull
  public CampaignContentEventLog persistEvent(
      @NotNull Long campaignContentId,
      @NotNull CampaignContentEventType eventType,
      @Nullable String visitorId,
      @Nullable Long userId,
      @Nullable String ipAddress,
      @Nullable String deviceType,
      @NotNull LocalDateTime occurredAt) {
    CampaignContentEventLog event =
        CampaignContentEventLog.builder()
            .campaignContentId(campaignContentId)
            .eventType(eventType)
            .visitorId(visitorId)
            .userId(userId)
            .ipAddress(ipAddress)
            .deviceType(deviceType)
            .occurredAt(occurredAt)
            .build();
    em.persist(event);
    em.flush();
    return event;
  }
}
