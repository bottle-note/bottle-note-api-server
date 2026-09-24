package app.bottlenote.mfds.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/** 부작용 없는 일괄 적용 미리보기. previewToken은 Redis에 저장하는 난수 발급 식별자이고, 상태 해시는 그 기록 안에서 따로 다시 계산한다. */
public record MfdsBulkMatchingPreviewResponse(
    Long sourceDeclarationId,
    Long alcoholId,
    String alcoholNameKo,
    String alcoholNameEn,
    Long distilleryId,
    Long regionId,
    String previewToken,
    LocalDateTime previewExpiresAt,
    int applicableCount,
    int unchangedCount,
    int reviewCount,
    int conflictCount,
    List<MfdsBulkMatchingPreviewItem> items) {}
