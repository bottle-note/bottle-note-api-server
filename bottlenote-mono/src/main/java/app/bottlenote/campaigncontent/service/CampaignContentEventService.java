package app.bottlenote.campaigncontent.service;

import static app.bottlenote.campaigncontent.exception.CampaignContentExceptionCode.CAMPAIGN_CONTENT_NOT_FOUND;

import app.bottlenote.campaigncontent.constant.CampaignContentEventType;
import app.bottlenote.campaigncontent.domain.CampaignContent;
import app.bottlenote.campaigncontent.domain.CampaignContentEventLog;
import app.bottlenote.campaigncontent.domain.CampaignContentEventRepository;
import app.bottlenote.campaigncontent.domain.CampaignContentRepository;
import app.bottlenote.campaigncontent.dto.request.CampaignContentEventContextRequest;
import app.bottlenote.campaigncontent.dto.response.CampaignContentEventResponse;
import app.bottlenote.campaigncontent.exception.CampaignContentException;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CampaignContentEventService {

  private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

  private final CampaignContentRepository campaignContentRepository;
  private final CampaignContentEventRepository campaignContentEventRepository;
  private final Clock clock;

  @Transactional
  public CampaignContentEventResponse registerEvent(
      String code, CampaignContentEventType type, CampaignContentEventContextRequest context) {
    // 로그인이 필요한 요청은 기존 API와 같은 REQUIRED_USER_ID(400)로 응답한다.
    if (type.requiresLogin() && context.userId() == null) {
      throw new UserException(UserExceptionCode.REQUIRED_USER_ID);
    }
    // 비활성 코드는 미등록과 같게 응답해 운영하지 않는 콘텐츠를 드러내지 않는다.
    CampaignContent campaignContent =
        campaignContentRepository
            .findByCode(code)
            .filter(content -> Boolean.TRUE.equals(content.getIsActive()))
            .orElseThrow(() -> new CampaignContentException(CAMPAIGN_CONTENT_NOT_FOUND));

    CampaignContentEventLog event =
        campaignContentEventRepository.register(
            CampaignContentEventLog.builder()
                .campaignContentId(campaignContent.getId())
                .eventType(type)
                .visitorId(context.visitorId())
                .userId(context.userId())
                .ipAddress(context.ipAddress())
                .deviceType(context.deviceType())
                .occurredAt(LocalDateTime.now(clock.withZone(ZONE)))
                .build());
    return new CampaignContentEventResponse(
        campaignContent.getCode(), event.getEventType(), event.getOccurredAt());
  }
}
