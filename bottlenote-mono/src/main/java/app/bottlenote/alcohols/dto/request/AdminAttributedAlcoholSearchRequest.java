package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.AttributeType;
import app.bottlenote.alcohols.dto.dsl.AlcoholAttributeSearchCondition;
import lombok.Builder;

/**
 * 속성이 붙은 위스키 목록 요청.
 *
 * @param keyword 위스키 이름 검색 (한글/영문)
 * @param type 속성 분류
 * @param key 속성 항목
 * @param value 속성 값 (정확히 일치)
 * @param missingKey 이 항목이 없는 위스키만
 * @param page 페이지 번호 (0부터)
 * @param size 페이지 크기 (기본 20, 최대 100)
 */
public record AdminAttributedAlcoholSearchRequest(
    String keyword,
    AttributeType type,
    String key,
    String value,
    String missingKey,
    Integer page,
    Integer size) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  @Builder
  public AdminAttributedAlcoholSearchRequest {
    page = page != null && page >= 0 ? page : 0;
    if (size == null || size < 1) {
      size = DEFAULT_SIZE;
    } else {
      size = Math.min(size, MAX_SIZE);
    }
  }

  public AlcoholAttributeSearchCondition toCondition() {
    return new AlcoholAttributeSearchCondition(keyword, type, key, value, missingKey);
  }
}
