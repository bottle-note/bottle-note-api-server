package app.bottlenote.alcohols.dto.request;

import app.bottlenote.alcohols.constant.TastingTagGroupRole;
import jakarta.validation.constraints.NotNull;

/** 맛 그룹에 이미 들어 있는 태그의 역할만 바꾼다. */
public record AdminTastingTagGroupTagRoleRequest(
    @NotNull(message = "TASTING_TAG_GROUP_ROLE_REQUIRED") TastingTagGroupRole role) {}
