package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.dto.request.AdminAlcoholBulkRowRequest;
import java.util.List;

public record AdminAlcoholBulkRowItem(
    String clientRowId,
    boolean valid,
    AdminAlcoholBulkRowRequest normalized,
    List<AdminAlcoholBulkIssueItem> errors,
    List<AdminAlcoholBulkIssueItem> warnings,
    List<Long> candidateAlcoholIds) {}
