package app.bottlenote.alcohols.dto.response;

import java.util.List;

/** 마트료시카 스타일 테이스팅 태그 트리 노드. depth 3 고정, 반대방향 필드는 null로 설정. */
public record TastingTagNodeItem(
    Long id,
    String korName,
    String engName,
    String icon,
    String description,
    TastingTagNodeItem parent,
    List<TastingTagNodeItem> children) {

  public static TastingTagNodeItem of(
      Long id,
      String korName,
      String engName,
      String icon,
      String description,
      TastingTagNodeItem parent,
      List<TastingTagNodeItem> children) {
    return new TastingTagNodeItem(id, korName, engName, icon, description, parent, children);
  }

  /** 목록 조회용 (parent/children = null) */
  public static TastingTagNodeItem forList(
      Long id, String korName, String engName, String icon, String description) {
    return new TastingTagNodeItem(id, korName, engName, icon, description, null, null);
  }
}
