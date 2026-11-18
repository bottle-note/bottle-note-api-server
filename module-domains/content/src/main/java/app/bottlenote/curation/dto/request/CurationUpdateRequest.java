package app.bottlenote.curation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CurationUpdateRequest(
    @NotNull(message = "CURATION_SPEC_ID_REQUIRED") Long specId,
    @NotBlank(message = "CURATION_NAME_REQUIRED") String name,
    String description,
    @NotEmpty(message = "CURATION_IMAGE_URLS_REQUIRED")
        @Size(max = 3, message = "CURATION_IMAGE_URLS_MAX_SIZE")
        List<String> imageUrls,
    LocalDate exposureStartDate,
    LocalDate exposureEndDate,
    @NotNull(message = "CURATION_DISPLAY_ORDER_REQUIRED") Integer displayOrder,
    @NotNull(message = "CURATION_IS_ACTIVE_REQUIRED") Boolean isActive,
    @NotNull(message = "CURATION_PAYLOAD_REQUIRED") Object payload) {}
