package app.bottlenote.alcohols.dto.response;

/** 테이스팅 태그별로 붙은 위스키 수. JPQL 생성자 projection 대상이라 public 최상위 record로 둔다. */
public record TastingTagAlcoholCountItem(Long tastingTagId, Long alcoholCount) {}
