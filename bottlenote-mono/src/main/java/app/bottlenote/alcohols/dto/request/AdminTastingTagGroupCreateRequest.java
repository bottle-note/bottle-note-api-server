package app.bottlenote.alcohols.dto.request;

import jakarta.validation.constraints.NotBlank;

/** 맛 그룹 생성 요청. 필수 여부는 여기서, 코드·이름 형식은 도메인이 검증한다. */
public record AdminTastingTagGroupCreateRequest(
    @NotBlank(message = "TASTING_TAG_GROUP_CODE_REQUIRED") String code,
    @NotBlank(message = "TASTING_TAG_GROUP_NAME_REQUIRED") String name,
    String family,
    String description,
    Integer displayOrder) {}
