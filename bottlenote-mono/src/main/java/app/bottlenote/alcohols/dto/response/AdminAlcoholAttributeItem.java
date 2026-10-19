package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import java.time.LocalDateTime;

public record AdminAlcoholAttributeItem(
    Long id,
    String key,
    String displayName,
    AttributeType type,
    AttributeDatatype datatype,
    String unit,
    String description,
    LocalDateTime createAt,
    LocalDateTime lastModifyAt,
    AdminAuditPrincipalItem createPrincipal,
    AdminAuditPrincipalItem lastModifyPrincipal) {}
