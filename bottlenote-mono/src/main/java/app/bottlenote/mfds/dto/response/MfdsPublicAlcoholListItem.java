package app.bottlenote.mfds.dto.response;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MfdsPublicAlcoholListItem(
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
    Short ageYears,
    @NotNull boolean distilleryLinked,
    @NotNull boolean regionLinked,
    String exportCountryAlpha2,
    String exportCountryNameKo,
    Integer volumeMl,
    BigDecimal abvPercent,
    Long importerId,
    String importerBaseName) {}
