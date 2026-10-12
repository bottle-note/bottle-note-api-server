package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import java.time.LocalDateTime;

/** 위스키 속성 한 행. */
public record AdminAlcoholAttributeItem(
    Long id,
    Long alcoholId,
    String key,
    AttributeType type,
    AttributeDatatype datatype,
    String value,
    String description,
    LocalDateTime basedAt,
    LocalDateTime createAt,
    LocalDateTime lastModifyAt,
    AdminAuditPrincipalItem createPrincipal,
    AdminAuditPrincipalItem lastModifyPrincipal) {}
