package app.bottlenote.alcohols.dto.dsl;

import app.bottlenote.alcohols.constant.AttributeType;

/**
 * 속성이 붙은 위스키를 찾는 조건. 비어 있는 조건은 무시한다.
 *
 * @param keyword 위스키 한글·영문 이름 포함 검색
 * @param type 속성 분류
 * @param key 속성 항목
 * @param value 속성 값(정확히 일치)
 * @param missingKey 이 항목이 없는 위스키만
 */
public record AlcoholAttributeSearchCondition(
    String keyword, AttributeType type, String key, String value, String missingKey) {

  public AlcoholAttributeSearchCondition {
    keyword = blankToNull(keyword);
    key = blankToNull(key);
    value = blankToNull(value);
    missingKey = blankToNull(missingKey);
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.trim();
  }
}
