package app.bottlenote.alcohols.dto.response;

import java.time.LocalDateTime;

public record AdminAlcoholAttributeGroupItem(
    Long id,
    String name,
    String description,
    int displayOrder,
    LocalDateTime createAt,
    LocalDateTime lastModifyAt,
    AdminAuditPrincipalItem createPrincipal,
    AdminAuditPrincipalItem lastModifyPrincipal) {}
