package app.bottlenote.alcohols.dto.response;

/** 맛 그룹에 연결된 태그 한 건. */
public record AdminTastingTagGroupTagItem(
    Long tastingTagId,
    String korName,
    AdminCodeLabelItem role,
    AdminCodeLabelItem provider,
    long alcoholCount) {}
