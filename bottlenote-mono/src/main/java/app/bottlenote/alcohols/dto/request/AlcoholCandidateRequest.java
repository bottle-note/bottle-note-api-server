package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup;
import app.bottlenote.alcohols.constant.AttributeSearchOperator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/** 그룹 후보 필터. include는 AND, exclude는 하나라도 만족하면 술 전체 제외. */
public record AlcoholCandidateRequest(
    @Size(max = 200) String keyword,
    @Size(max = 20) List<@NotNull @Valid Condition> include,
    @Size(max = 20) List<@NotNull @Valid Condition> exclude,
    @Size(max = 6) List<@NotNull AlcoholCategoryGroup> categoryGroups,
    @DecimalMin("0") @DecimalMax("100") BigDecimal abvFrom,
    @DecimalMin("0") @DecimalMax("100") BigDecimal abvTo,
    @Size(max = 20) List<@NotBlank @Size(max = 64) String> tastingGroupCodes,
    @Valid NextQuestion nextQuestion,
    @Size(max = 4096) String cursor,
    @Min(1) @Max(100) Integer size) {

  public AlcoholCandidateRequest {
    include = include == null ? List.of() : List.copyOf(include);
    exclude = exclude == null ? List.of() : List.copyOf(exclude);
    categoryGroups = categoryGroups == null ? List.of() : List.copyOf(categoryGroups);
    tastingGroupCodes = tastingGroupCodes == null ? List.of() : List.copyOf(tastingGroupCodes);
    size = size == null ? 25 : size;
    keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
    cursor = cursor == null || cursor.isBlank() ? null : cursor;
  }

  public record Condition(
      @NotNull @Positive Long attributeId,
      @NotNull AttributeSearchOperator operator,
      @Size(max = 255) String value) {}

  public enum Source {
    ATTRIBUTE,
    CATEGORY,
    ABV,
    TASTING_GROUP
  }

  public record NextQuestion(
      @NotNull Source source,
      @Positive Long attributeId,
      @Size(max = 20) List<@NotNull @Valid Range> ranges) {
    public NextQuestion {
      ranges = ranges == null ? List.of() : List.copyOf(ranges);
    }
  }

  public record Range(@NotBlank @Size(max = 64) String key, BigDecimal from, BigDecimal to) {}
}
