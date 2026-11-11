package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

public record AdminAlcoholMergeRequest(
    @NotNull(message = "ALCOHOL_MERGE_BASE_REQUIRED") Long baseAlcoholId,
    @NotNull(message = "ALCOHOL_MERGE_DUPLICATES_REQUIRED")
        @Size(min = 1, max = 10, message = "ALCOHOL_MERGE_DUPLICATES_SIZE")
        @UniqueElements(message = "ALCOHOL_MERGE_DUPLICATES_NOT_UNIQUE")
        List<@NotNull(message = "ALCOHOL_MERGE_DUPLICATES_REQUIRED") Long> duplicateAlcoholIds) {}
