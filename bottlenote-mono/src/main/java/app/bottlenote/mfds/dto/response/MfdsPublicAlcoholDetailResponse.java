package app.bottlenote.mfds.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MfdsPublicAlcoholDetailResponse(
    Long id,
    String rcno,
    LocalDate processedDate,
    Long alcoholId,
    String alcoholNameKo,
    String alcoholNameEn,
    String baseProductNameKo,
    String baseProductNameEn,
    String skuDisplayNameKo,
    String skuDisplayNameEn,
    String alcoholCategoryKo,
    String alcoholCategoryEn,
    String exportCountryAlpha2,
    String exportCountryNameKo,
    Integer volumeMl,
    BigDecimal abvPercent,
    Long importerId,
    String importerBaseName,
    Integer unitVolumeMl,
    Integer packageCount,
    Short ageYears,
    Short vintageYear,
    String editionName,
    String caskNumber,
    String batchNumber,
    LocalDate expiryStart,
    LocalDate expiryEnd,
    String manufacturerName,
    String manufactureCountryNameKo,
    MfdsPublicImporterItem importer) {}
