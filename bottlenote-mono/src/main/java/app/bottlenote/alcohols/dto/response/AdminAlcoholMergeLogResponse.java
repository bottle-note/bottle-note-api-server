package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.domain.AlcoholMergeCount;
import java.time.LocalDateTime;
import java.util.List;

public record AdminAlcoholMergeLogResponse(
    Long mergeId,
    String mergeGroupId,
    Long baseAlcoholId,
    String baseKorName,
    Long duplicateAlcoholId,
    String duplicateKorName,
    String duplicateEngName,
    List<AlcoholMergeCount> counts,
    Long mergedByAdminId,
    String mergedByEmail,
    LocalDateTime mergedAt) {}
