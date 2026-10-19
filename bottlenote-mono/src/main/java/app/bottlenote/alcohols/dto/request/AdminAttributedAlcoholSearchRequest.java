package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AttributeOperator;
import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.global.dto.request.OffsetPageRequest;
import jakarta.validation.constraints.Pattern;
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
    Integer size) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  @Builder
  public AdminAttributedAlcoholSearchRequest {
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
}
