package app.bottlenote.statistics.dto.request;

import java.time.LocalDate;

/** from·to를 생략하면 오늘 기준 최근 30일이다. */
public record MemberFunnelRequest(LocalDate from, LocalDate to) {}
