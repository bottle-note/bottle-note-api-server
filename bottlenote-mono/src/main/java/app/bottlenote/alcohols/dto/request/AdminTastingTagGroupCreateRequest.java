package app.bottlenote.alcohols.dto.request;

/** 맛 그룹 생성 요청. 코드·이름 형식은 도메인이 검증한다. */
public record AdminTastingTagGroupCreateRequest(
    String code, String name, String family, String description, Integer displayOrder) {}
