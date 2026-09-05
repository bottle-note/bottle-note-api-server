package app.bottlenote.alcohols.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminAlcoholBulkRequest(
    @NotEmpty(message = "ALCOHOL_BULK_ROWS_REQUIRED")
        @Size(max = MAX_ROWS, message = "ALCOHOL_BULK_ROWS_MAX_SIZE")
        List<@NotNull(message = "ALCOHOL_BULK_ROW_REQUIRED") @Valid AdminAlcoholBulkRowRequest>
            rows) {
  public static final int MAX_ROWS = 1000;
}
