package app.bottlenote.alcohols.dto.response;

/** 태그가 속한 맛 그룹 하나. role은 코드와 화면 표기를 함께 담는다. */
public record AdminTastingTagGroupRefItem(
    String groupCode, String groupName, AdminCodeLabelItem role) {}
