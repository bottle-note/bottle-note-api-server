package app.bottlenote.campaigncontent.dto.request;

import java.time.LocalDate;

/** 조회 기간(양 끝 포함, Asia/Seoul). 비우면 서비스가 최근 7일로 채운다. */
public record AdminCampaignContentMetricsRequest(LocalDate from, LocalDate to) {}
