package app.bottlenote.alcohols.dto.response;

import java.util.List;

/** 맛 그룹 전체와, 그룹에 등장하는 계열의 distinct 목록. */
public record AdminTastingTagGroupListResponse(
    List<AdminTastingTagGroupItem> groups, List<String> families) {}
