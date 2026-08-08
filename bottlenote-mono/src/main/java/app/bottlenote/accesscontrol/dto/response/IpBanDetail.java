package app.bottlenote.accesscontrol.dto.response;

import app.bottlenote.accesscontrol.constant.IpBanStatus;
import java.time.LocalDateTime;
import java.util.List;

/** IP 차단 상세와 감사 이력. */
public record IpBanDetail(
    Long id,
    String normalizedIp,
    IpBanStatus status,
    String reason,
    LocalDateTime effectiveFrom,
    LocalDateTime expiresAt,
    LocalDateTime stateChangedAt,
    List<IpBanEventView> events) {}
