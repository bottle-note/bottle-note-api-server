package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import java.time.LocalDateTime;
import java.util.List;

public record AdminAlcoholMergeLogResponse(
    Long mergeId,
    String mergeGroupId,
    Long targetAlcoholId,
    String targetKorName,
    Long sourceAlcoholId,
    String sourceKorName,
    String sourceEngName,
    List<AlcoholMergeCount> counts,
    Long mergedByAdminId,
    String mergedByEmail,
    LocalDateTime mergedAt) {}
