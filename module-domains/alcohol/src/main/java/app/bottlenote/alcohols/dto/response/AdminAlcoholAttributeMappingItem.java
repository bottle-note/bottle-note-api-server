package app.bottlenote.alcohols.dto.response;

import java.time.LocalDateTime;

public record AdminAlcoholAttributeMappingItem(
    Long id,
    Long alcoholId,
    Long attributeId,
    String value,
    LocalDateTime createAt,
    LocalDateTime lastModifyAt,
    AdminAuditPrincipalItem createPrincipal,
    AdminAuditPrincipalItem lastModifyPrincipal) {}
