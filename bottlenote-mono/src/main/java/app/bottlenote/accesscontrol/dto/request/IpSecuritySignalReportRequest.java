package app.bottlenote.accesscontrol.dto.request;

import java.time.LocalDateTime;

/** 민감한 요청 원문 없이 저장하는 IP 보안 signal 입력값. */
public record IpSecuritySignalReportRequest(
    Long ipBanId,
    String rawIp,
    String endpointPath,
    String httpMethod,
    String ruleCode,
    LocalDateTime observedFrom,
    LocalDateTime observedUntil,
    int observationCount,
    String agentVersion) {}
