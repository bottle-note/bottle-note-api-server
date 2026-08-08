package app.bottlenote.accesscontrol.dto.response;

import app.bottlenote.accesscontrol.constant.IpBanStatus;
import java.time.LocalDateTime;

/** IP 차단 목록용 요약. */
public record IpBanSummaryResponse(
    Long id,
    String normalizedIp,
    IpBanStatus status,
    String reason,
    LocalDateTime effectiveFrom,
    LocalDateTime expiresAt,
    LocalDateTime stateChangedAt) {}
