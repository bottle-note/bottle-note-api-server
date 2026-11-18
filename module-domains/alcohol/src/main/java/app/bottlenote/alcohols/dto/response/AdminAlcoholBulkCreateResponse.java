package app.bottlenote.alcohols.dto.response;

import java.util.List;

public record AdminAlcoholBulkCreateResponse(
    int createdRows, List<CreatedRow> rows, AdminAlcoholBulkValidateResponse validation) {
  public record CreatedRow(String clientRowId, Long alcoholId) {}
}
