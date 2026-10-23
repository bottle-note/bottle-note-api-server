package app.bottlenote.alcohols.dto.dsl;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCandidateRequest.Condition;
import app.bottlenote.alcohols.dto.request.AdminAlcoholCandidateRequest.NextQuestion;
import java.math.BigDecimal;
import java.util.List;

public record AlcoholCandidateCriteria(
    Long groupId,
    String keyword,
    List<Condition> include,
    List<Condition> exclude,
    List<BaseFilterCriteria> filters,
    NextQuestion nextQuestion,
    int page,
    int size) {
  public record BaseFilterCriteria(
      List<AlcoholCategoryGroup> categories,
      BigDecimal abvFrom,
      BigDecimal abvTo,
      List<Long> tastingTagIds,
      boolean requiresTasting) {}
}
