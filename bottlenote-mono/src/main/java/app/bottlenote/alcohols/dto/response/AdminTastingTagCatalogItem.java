package app.bottlenote.alcohols.dto.response;

import java.util.List;

/** 아이콘을 뺀 테이스팅 태그 사전 항목. */
public record AdminTastingTagCatalogItem(
    Long id,
    String korName,
    String engName,
    Long parentId,
    long alcoholCount,
    List<AdminTastingTagGroupRefItem> groups) {}
