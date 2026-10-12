package app.bottlenote.alcohols.dto.request;

/** 맛 그룹 수정 요청. 코드는 경로의 값이며 바꾸지 않는다. */
public record AdminTastingTagGroupUpdateRequest(
    String name, String family, String description, Integer displayOrder) {}
