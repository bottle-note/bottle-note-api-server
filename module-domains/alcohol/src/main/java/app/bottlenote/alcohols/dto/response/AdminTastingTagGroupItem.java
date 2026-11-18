package app.bottlenote.alcohols.dto.response;

import java.util.List;

/** 맛 그룹 한 건. 목록과 상세가 같은 모양이다. */
public record AdminTastingTagGroupItem(
    Long id,
    String code,
    String name,
    String family,
    String description,
    Integer displayOrder,
    List<AdminTastingTagGroupTagItem> tags) {}
