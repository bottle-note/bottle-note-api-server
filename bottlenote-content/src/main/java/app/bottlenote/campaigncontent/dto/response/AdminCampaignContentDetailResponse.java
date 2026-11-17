package app.bottlenote.campaigncontent.dto.response;

import java.time.LocalDateTime;

public record AdminCampaignContentDetailResponse(
    Long id,
    String code,
    String name,
    String description,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt) {}
