package app.bottlenote.campaigncontent.dto.request;

import app.bottlenote.campaigncontent.constant.CampaignContentEventType;
import jakarta.validation.constraints.NotNull;

/**
 * @param type 이벤트 유형. RESULT는 로그인 상태에서만 보낼 수 있다
 */
public record CampaignContentEventRequest(
    @NotNull(message = "이벤트 유형은 필수입니다.") CampaignContentEventType type) {}
