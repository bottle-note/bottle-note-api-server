package app.bottlenote.mfds.dto.response;

import java.time.LocalDate;
import java.util.List;

/** 일괄 적용 미리보기의 신고 한 건. */
public record MfdsBulkMatchingPreviewItem(
    Long declarationId,
    String rcno,
    String displayName,
    Integer volumeMl,
    String importerBaseName,
    LocalDate processedDate,
    String classification,
    List<MfdsBulkMatchingReasonItem> reasons,
    Long currentAlcoholId,
    Long currentDistilleryId,
    Long currentRegionId) {}
