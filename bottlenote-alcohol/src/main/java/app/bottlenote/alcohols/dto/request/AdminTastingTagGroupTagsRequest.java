package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.TastingTagGroupRole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 맛 그룹에 태그를 넣거나, 이미 있으면 역할만 바꾼다. */
public record AdminTastingTagGroupTagsRequest(
    @NotEmpty(message = "TASTING_TAG_GROUP_TAG_IDS_REQUIRED")
        List<@NotNull(message = "TASTING_TAG_GROUP_TAG_ID_REQUIRED") Long> tastingTagIds,
    @NotNull(message = "TASTING_TAG_GROUP_ROLE_REQUIRED") TastingTagGroupRole role) {}
