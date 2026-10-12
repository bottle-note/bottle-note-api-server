package app.bottlenote.alcohols.dto.response;

import app.bottlenote.alcohols.constant.AttributeDatatype;
import app.bottlenote.alcohols.constant.AttributeType;

/** 전체 속성에서 항목별로 모은 집계. */
public record AdminAlcoholAttributeKeyItem(
    String key, AttributeType type, AttributeDatatype datatype, long count, long alcoholCount) {}
