package app.bottlenote.campaigncontent.dto.response;

import java.time.LocalDateTime;

/**
 * @param recentParticipants 오늘을 포함한 최근 7일간 결과를 조회한 순회원 수
 */
public record AdminCampaignContentListResponse(
    Long id,
    String code,
    String name,
    String description,
    Long recentParticipants,
    Boolean isActive,
    LocalDateTime createdAt) {}
