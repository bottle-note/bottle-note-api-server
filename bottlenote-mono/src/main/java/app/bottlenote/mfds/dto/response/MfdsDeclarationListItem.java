package app.bottlenote.mfds.dto.response;

import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import app.bottlenote.mfds.constant.MfdsMatchSelectionSource;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 수입 신고 목록 응답 항목. */
public record MfdsDeclarationListItem(
    Long id,
    String rcno,
    String baseProductNameKo,
    String baseProductNameEn,
    Integer volumeMl,
    BigDecimal abvPercent,
    MfdsNormalizationStatus normalizationStatus,
    Long importerId,
    String importerBaseName,
    MfdsImporterLinkSource importerLinkSource,
    Long selectedAlcoholId,
    MfdsMatchSelectionSource alcoholMatchDecision,
    LocalDateTime matchedAt,
    LocalDateTime createdAt) {}
