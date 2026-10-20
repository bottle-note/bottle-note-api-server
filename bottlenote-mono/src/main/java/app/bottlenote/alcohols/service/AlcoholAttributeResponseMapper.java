package app.bottlenote.alcohols.service;

import app.bottlenote.alcohols.domain.AlcoholAttribute;
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeItem;
import app.bottlenote.alcohols.dto.response.AdminAuditPrincipalItem;
import app.bottlenote.common.domain.AuditPrincipal;

final class AlcoholAttributeResponseMapper {
  private AlcoholAttributeResponseMapper() {}

  static AdminAlcoholAttributeItem toItem(AlcoholAttribute a) {
    return new AdminAlcoholAttributeItem(
        a.getId(),
        a.getGroupId(),
        a.getAttributeKey(),
        a.getDisplayName(),
        a.getAttributeType(),
        a.getDatatype(),
        a.getUnit(),
        a.getDescription(),
        a.getAllowedValues(),
        a.getPattern(),
        a.getCreateAt(),
        a.getLastModifyAt(),
        toPrincipal(a.getCreatePrincipal()),
        toPrincipal(a.getLastModifyPrincipal()));
  }

  static AdminAuditPrincipalItem toPrincipal(AuditPrincipal principal) {
    return principal == null
        ? null
        : new AdminAuditPrincipalItem(principal.getId(), principal.getType(), principal.getEmail());
  }
}
