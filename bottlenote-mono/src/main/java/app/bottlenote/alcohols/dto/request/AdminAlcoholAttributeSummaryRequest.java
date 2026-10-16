package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record AdminAlcoholAttributeSummaryRequest(
    @Size(max = 500, message = "ALCOHOL_ATTRIBUTE_BATCH_SIZE_INVALID")
        List<
                @NotNull(message = "ALCOHOL_ID_REQUIRED") @Positive(message = "ALCOHOL_ID_MINIMUM")
                Long>
            alcoholIds) {
  public AdminAlcoholAttributeSummaryRequest {
    alcoholIds =
        alcoholIds == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(alcoholIds));
  }
}
