package app.bottlenote.support.report.dto;

import java.time.LocalDate;

public record DailyDataReportDto(
    LocalDate reportDate,
    Long newUsersCount,
    Long newReviewsCount,
    Long newRepliesCount,
    Long newLikesCount,
    Boolean webhookSent) {}
