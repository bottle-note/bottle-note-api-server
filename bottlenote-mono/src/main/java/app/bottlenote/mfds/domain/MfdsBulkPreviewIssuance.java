package app.bottlenote.mfds.domain;

import java.time.LocalDateTime;

/** 서버가 발급한 일괄 미리보기. 상태 해시와 관리자 범위를 담으며 클라이언트 해시 계산을 대신하지 않는다. */
public record MfdsBulkPreviewIssuance(
    String token,
    Long adminId,
    Long sourceDeclarationId,
    Long alcoholId,
    Long distilleryId,
    Long regionId,
    LocalDateTime expiresAt,
    String stateHash) {}
