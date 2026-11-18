package app.bottlenote.alcohols.dto.dsl;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.AlcoholQuestionSource;
import app.bottlenote.alcohols.constant.AttributeSearchOperator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record AlcoholCandidateCriteria(
    Long groupId,
    String keyword,
    List<ConditionCriteria> include,
    List<ConditionCriteria> exclude,
    List<BaseFilterCriteria> filters,
    NextQuestionCriteria nextQuestion,
    int page,
    int size) {
  public record BaseFilterCriteria(
      List<AlcoholCategoryGroup> categories,
      BigDecimal abvFrom,
      BigDecimal abvTo,
      List<Long> tastingTagIds,
      boolean requiresTasting,
      boolean abvFromExclusive) {
    public BaseFilterCriteria(
        List<AlcoholCategoryGroup> categories,
        BigDecimal abvFrom,
        BigDecimal abvTo,
        List<Long> tastingTagIds,
        boolean requiresTasting) {
      this(categories, abvFrom, abvTo, tastingTagIds, requiresTasting, false);
    }
  }

  public record ConditionCriteria(
      @NotNull @Positive Long attributeId,
      @NotNull AttributeSearchOperator operator,
      @Size(max = 255) String value) {}

  public record NextQuestionCriteria(
      @NotNull AlcoholQuestionSource source,
      @Positive Long attributeId,
      @Size(max = 20) List<@NotNull @Valid RangeCriteria> ranges) {
    public NextQuestionCriteria {
      ranges = ranges == null ? List.of() : List.copyOf(ranges);
    }
  }

  public record RangeCriteria(
      @NotBlank @Size(max = 64) String key, BigDecimal from, BigDecimal to) {}
}
