package app.bottlenote.support.report.dto.request;

import app.bottlenote.support.report.constant.ReviewReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewReportRequest(
    @NotNull(message = "REPORT_TARGET_REVIEW_ID_REQUIRED") Long reportReviewId,
    @NotNull(message = "REPORT_TYPE_NOT_VALID") ReviewReportType type,
    @NotBlank(message = "CONTENT_NOT_BLANK")
        @Size(min = 10, max = 300, message = "INVALID_REPORT_CONTENT_SIZE")
        String content) {}
