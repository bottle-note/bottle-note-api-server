package app.bottlenote.alcohols.dto.response;

import java.util.List;

public record AdminAlcoholBulkValidateResponse(
    int totalRows,
    int validRows,
    int invalidRows,
    int warningRows,
    List<AdminAlcoholBulkRowResult> rows) {}
