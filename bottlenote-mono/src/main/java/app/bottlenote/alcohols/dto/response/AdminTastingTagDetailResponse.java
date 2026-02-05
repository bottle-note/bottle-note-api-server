package app.bottlenote.alcohols.dto.response;

import java.util.List;

/** 테이스팅 태그 상세 조회 응답. 마트료시카 스타일 트리 구조 + 연결된 위스키 목록. */
public record AdminTastingTagDetailResponse(
    TastingTagNodeItem tag, List<AdminAlcoholItem> alcohols) {

  public static AdminTastingTagDetailResponse of(
      TastingTagNodeItem tag, List<AdminAlcoholItem> alcohols) {
    return new AdminTastingTagDetailResponse(tag, alcohols);
  }
}
