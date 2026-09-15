package app.bottlenote.campaigncontent.dto.response;

import app.bottlenote.campaigncontent.constant.CampaignContentEventType;
import java.time.LocalDateTime;

/**
 * @param code 캠페인 콘텐츠 코드
 * @param type 이벤트 유형
 * @param occurredAt 서버가 기록한 발생 시각(Asia/Seoul)
 */
public record CampaignContentEventResponse(
    String code, CampaignContentEventType type, LocalDateTime occurredAt) {}
