package app.bottlenote.accesscontrol.dto.response;

import app.bottlenote.accesscontrol.constant.IpBanActorType;
import app.bottlenote.accesscontrol.constant.IpBanEventType;
import java.time.LocalDateTime;

/** IP 차단 감사 이벤트 조회 뷰. */
public record IpBanEventView(
    Long id,
    Long ipBanId,
    IpBanEventType eventType,
    String reason,
    LocalDateTime previousExpiresAt,
    LocalDateTime nextExpiresAt,
    IpBanActorType actorType,
    Long actorAdminUserId,
    String actorAgentId,
    LocalDateTime occurredAt) {}
