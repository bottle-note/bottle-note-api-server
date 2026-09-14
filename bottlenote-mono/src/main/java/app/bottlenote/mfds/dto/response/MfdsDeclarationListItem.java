package app.bottlenote.mfds.dto.response;

import app.bottlenote.mfds.constant.MfdsImporterLinkSource;
import app.bottlenote.mfds.constant.MfdsNormalizationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 수입 신고 목록 응답 항목. */
public record MfdsDeclarationListItem(
    Long id,
    String rcno,
    LocalDate processedDate,
    String baseProductNameKo,
    String baseProductNameEn,
    String skuDisplayNameKo,
    String skuDisplayNameEn,
    Integer volumeMl,
    BigDecimal abvPercent,
    Short ageYears,
    String alcoholCategoryKo,
    String alcoholCategoryEn,
    MfdsNormalizationStatus normalizationStatus,
    Long importerId,
    String importerBaseName,
    MfdsImporterLinkSource importerLinkSource,
    Long selectedAlcoholId,
    String alcoholMatchDecision,
    boolean distilleryLinked,
    boolean regionLinked,
    LocalDateTime matchedAt,
    LocalDateTime createdAt) {}
