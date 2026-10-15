package app.bottlenote.alcohols.dto.response;

import java.util.List;

/** 속성이 붙은 위스키 한 줄. 위스키 기본 정보와 그 위스키의 속성 행을 함께 담는다. */
public record AdminAttributedAlcoholItem(
    Long alcoholId,
    String korName,
    String engName,
    String korCategory,
    String imageUrl,
    List<AdminAlcoholAttributeItem> attributes) {}
