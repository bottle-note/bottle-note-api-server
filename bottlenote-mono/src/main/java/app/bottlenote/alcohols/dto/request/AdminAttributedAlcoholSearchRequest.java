package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AttributeOperator;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.global.dto.request.OffsetPageRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Builder;

/**
 * 속성이 붙은 위스키 목록 요청. 비어 있는 조건은 null로 정리해 무시한다.
 *
 * @param keyword 위스키 이름 검색 (한글/영문)
 * @param type 속성 분류
 * @param key 속성 항목
 * @param value 술별 속성 값
 * @param operator 검색 비교 조건. 기본 EQ, 숫자 자료형은 GTE 허용
 * @param missingKey 이 항목이 없는 위스키만
 * @param page 페이지 번호 (0부터)
 * @param size 페이지 크기 (기본 20, 최대 100)
 */
public record AdminAttributedAlcoholSearchRequest(
    String keyword,
    AttributeType type,
    @Pattern(regexp = "[a-z][a-z0-9_]{0,63}", message = "ALCOHOL_ATTRIBUTE_KEY_INVALID") String key,
    String value,
    AttributeOperator operator,
    @Pattern(regexp = "[a-z][a-z0-9_]{0,63}", message = "ALCOHOL_ATTRIBUTE_KEY_INVALID")
        String missingKey,
    Integer page,
    Integer size,
    @Size(max = 100, message = "ALCOHOL_ATTRIBUTE_GROUP_IDS_SIZE_INVALID")
        List<
                @NotNull(message = "ALCOHOL_ATTRIBUTE_GROUP_INVALID_VALUE")
                @Positive(message = "ALCOHOL_ATTRIBUTE_GROUP_INVALID_VALUE") Long>
            groupIds) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  @Builder
  @org.springframework.beans.factory.annotation.Autowired
  public AdminAttributedAlcoholSearchRequest {
    groupIds =
        groupIds == null
            ? List.of()
            : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(groupIds));
    operator = operator == null ? AttributeOperator.EQ : operator;
    keyword = blankToNull(keyword);
    key = blankToNull(key);
    value = blankToNull(value);
    missingKey = blankToNull(missingKey);
    var pagination = OffsetPageRequest.of(page, size, DEFAULT_SIZE, MAX_SIZE);
    page = pagination.page();
    size = pagination.size();
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.trim();
  }

  public AdminAttributedAlcoholSearchRequest(
      String keyword,
      AttributeType type,
      String key,
      String value,
      AttributeOperator operator,
      String missingKey,
      Integer page,
      Integer size) {
    this(keyword, type, key, value, operator, missingKey, page, size, List.of());
  }
}
