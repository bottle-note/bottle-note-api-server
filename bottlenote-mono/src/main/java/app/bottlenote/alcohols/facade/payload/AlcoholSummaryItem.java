package app.bottlenote.alcohols.facade.payload;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public record AlcoholSummaryItem(
    Long alcoholId,
    String korName,
    String engName,
    String korCategoryName,
    String engCategoryName,
    String imageUrl,
    Boolean isPicked) {
  public static AlcoholSummaryItem empty() {
    log.error("데이터가 불일치합니다. 데이터 정합성 확인이 필요합니다");
    return new AlcoholSummaryItem(null, null, null, null, null, null, null);
  }
}
