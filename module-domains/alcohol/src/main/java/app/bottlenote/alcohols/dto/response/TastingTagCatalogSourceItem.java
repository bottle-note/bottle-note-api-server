package app.bottlenote.alcohols.dto.response;

/** 아이콘 컬럼을 읽지 않는 태그 사전 원본. */
public record TastingTagCatalogSourceItem(Long id, String korName, String engName, Long parentId) {}
