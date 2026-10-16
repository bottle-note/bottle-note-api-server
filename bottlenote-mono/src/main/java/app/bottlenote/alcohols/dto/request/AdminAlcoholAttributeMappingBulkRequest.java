package app.bottlenote.alcohols.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminAlcoholAttributeMappingBulkRequest(
    @NotEmpty(message = "ALCOHOL_ATTRIBUTE_BATCH_SIZE_INVALID")
        @Size(max = 500, message = "ALCOHOL_ATTRIBUTE_BATCH_SIZE_INVALID")
        List<@NotNull(message = "ALCOHOL_ATTRIBUTE_ITEM_REQUIRED") @Valid Item> items) {
  public record Item(
      @NotNull(message = "ALCOHOL_ID_REQUIRED") @Positive(message = "ALCOHOL_ID_MINIMUM")
          Long alcoholId,
      @NotNull(message = "ALCOHOL_ATTRIBUTE_ID_REQUIRED")
          @Positive(message = "ALCOHOL_ATTRIBUTE_ID_INVALID")
          Long attributeId) {}
}
