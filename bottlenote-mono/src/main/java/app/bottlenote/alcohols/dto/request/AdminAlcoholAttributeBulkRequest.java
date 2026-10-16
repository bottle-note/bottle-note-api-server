package app.bottlenote.alcohols.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminAlcoholAttributeBulkRequest(
    @NotEmpty(message = "ALCOHOL_ATTRIBUTE_BATCH_SIZE_INVALID")
        @Size(max = 500, message = "ALCOHOL_ATTRIBUTE_BATCH_SIZE_INVALID")
        List<
                @NotNull(message = "ALCOHOL_ATTRIBUTE_ITEM_REQUIRED") @Valid
                AdminAlcoholAttributeCreateRequest>
            items) {}
