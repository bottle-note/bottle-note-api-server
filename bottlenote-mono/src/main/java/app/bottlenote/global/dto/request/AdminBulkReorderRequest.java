package app.bottlenote.global.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminBulkReorderRequest(
    @NotEmpty(message = "BULK_REORDER_IDS_REQUIRED")
        @Size(max = 100, message = "BULK_REORDER_IDS_MAX_SIZE")
        List<
                @NotNull(message = "BULK_REORDER_ID_REQUIRED")
                @Min(value = 1, message = "BULK_REORDER_ID_MINIMUM") Long>
            ids) {}
