package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotBlank;

/** 맛 그룹 수정 요청. 코드는 경로의 값이며 바꾸지 않는다. 이름 필수 여부는 여기서 검증한다. */
public record AdminTastingTagGroupUpdateRequest(
    @NotBlank(message = "TASTING_TAG_GROUP_NAME_REQUIRED") String name,
    String family,
    String description,
    Integer displayOrder) {}
