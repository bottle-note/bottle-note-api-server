package app.bottlenote.mfds.dto.response;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MfdsItemDetailResponse(
    @NotNull Long id,
    @NotNull String rcno,
    @NotNull String queriedItemCode,
    @NotNull String queriedItemName,
    String productDivisionName,
    String importerName,
    String productNameKo,
    String productNameEn,
    String itemName,
    String overseasEstablishmentName,
    LocalDate processedDate,
    String expiryText,
    String manufactureCountryName,
    String exportCountryName,
    String detailHref,
    @NotNull LocalDateTime observedAt) {}
