package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 속성 여러 건을 한 번에 지우는 요청. */
public record AdminAlcoholAttributeBatchDeleteRequest(
    @Size(max = 500, message = "ALCOHOL_ATTRIBUTE_BATCH_SIZE_INVALID")
        @NotEmpty(message = "ALCOHOL_ATTRIBUTE_IDS_REQUIRED")
        List<
                @NotNull(message = "ALCOHOL_ATTRIBUTE_ID_REQUIRED")
                @Positive(message = "ALCOHOL_ATTRIBUTE_ID_INVALID") Long>
            ids) {}
