package app.bottlenote.accesscontrol.dto.response;

/** DB 명령 결과와 Redis enforcement projection 상태. */
public record IpBanCommandResult(IpBanDetail detail, ProjectionStatus projectionStatus) {}
