package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;
import java.time.LocalDateTime;
import java.util.List;

public record AdminAlcoholAttributeItem(
    Long id,
    Long groupId,
    String key,
    String displayName,
    AttributeType type,
    AttributeDatatype datatype,
    String unit,
    String description,
    List<String> allowedValues,
    String pattern,
    boolean allowMultipleValues,
    LocalDateTime createAt,
    LocalDateTime lastModifyAt,
    AdminAuditPrincipalItem createPrincipal,
    AdminAuditPrincipalItem lastModifyPrincipal) {}
