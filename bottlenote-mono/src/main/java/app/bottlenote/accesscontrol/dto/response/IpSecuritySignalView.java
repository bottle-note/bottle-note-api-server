package app.bottlenote.accesscontrol.dto.response;

import app.bottlenote.accesscontrol.constant.SignalVerdict;
import java.time.LocalDateTime;

public record IpSecuritySignalView(
    Long id,
    Long ipBanId,
    String normalizedIp,
    String endpointPath,
    String httpMethod,
    String ruleCode,
    LocalDateTime observedFrom,
    LocalDateTime observedUntil,
    int observationCount,
    Long reportedByAdminUserId,
    String reportedByAgentId,
    String agentVersion,
    SignalVerdict verdict,
    Long reviewedByAdminUserId,
    LocalDateTime reviewedAt,
    String reviewNote) {}
