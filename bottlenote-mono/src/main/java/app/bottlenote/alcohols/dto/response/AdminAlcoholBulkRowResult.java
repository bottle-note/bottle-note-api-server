package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.dto.request.AdminAlcoholBulkRowRequest;
import java.util.List;

public record AdminAlcoholBulkRowResult(
    String clientRowId,
    boolean valid,
    AdminAlcoholBulkRowRequest normalized,
    List<AdminAlcoholBulkIssue> errors,
    List<AdminAlcoholBulkIssue> warnings,
    List<Long> candidateAlcoholIds) {}
