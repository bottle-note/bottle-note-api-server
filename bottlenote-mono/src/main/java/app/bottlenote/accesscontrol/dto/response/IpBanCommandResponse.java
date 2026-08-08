package app.bottlenote.accesscontrol.dto.response;

import app.bottlenote.accesscontrol.constant.ProjectionStatus;

/** DB 명령 결과와 Redis enforcement projection 상태. */
public record IpBanCommandResponse(IpBanDetailResponse detail, ProjectionStatus projectionStatus) {}
