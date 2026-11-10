package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

public record AdminAlcoholMergeRequest(
    @NotNull(message = "ALCOHOL_MERGE_TARGET_REQUIRED") Long targetAlcoholId,
    @NotNull(message = "ALCOHOL_MERGE_SOURCES_REQUIRED")
        @Size(min = 1, max = 10, message = "ALCOHOL_MERGE_SOURCES_SIZE")
        @UniqueElements(message = "ALCOHOL_MERGE_SOURCES_DUPLICATED")
        List<@NotNull(message = "ALCOHOL_MERGE_SOURCES_REQUIRED") Long> sourceAlcoholIds) {}
