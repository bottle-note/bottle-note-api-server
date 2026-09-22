package app.bottlenote.review.constant;

/** 베스트 리뷰 선정 변동 종류. 변동이 없는 날은 로그를 남기지 않는다. */
public enum BestReviewChangeType {
  SELECTED("베스트 리뷰로 선정"),
  RELEASED("베스트 리뷰 해제");

  private final String description;

  BestReviewChangeType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
