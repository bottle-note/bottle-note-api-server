package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;

/** 속성 항목·분류·값 형식별 행 수와 위스키 수. */
public record AlcoholAttributeKeyCountItem(
    String key, AttributeType type, AttributeDatatype datatype, Long count, Long alcoholCount) {}
