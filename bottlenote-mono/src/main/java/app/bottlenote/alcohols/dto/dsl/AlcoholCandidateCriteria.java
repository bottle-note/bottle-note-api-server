package app.bottlenote.alcohols.dto.dsl;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCandidateRequest;
import java.math.BigDecimal;
import java.util.List;

public record AlcoholCandidateCriteria(
    Long groupId,
    AdminAlcoholCandidateRequest request,
    List<Long> tastingTagIds,
    List<BaseFilterCriteria> conjunctiveFilters) {
  public AlcoholCandidateCriteria(
      Long groupId, AdminAlcoholCandidateRequest request, List<Long> tastingTagIds) {
    this(groupId, request, tastingTagIds, List.of());
  }

  public record BaseFilterCriteria(
      List<AlcoholCategoryGroup> categories,
      BigDecimal abvFrom,
      BigDecimal abvTo,
      List<Long> tastingTagIds,
      boolean requiresTasting) {}
}
