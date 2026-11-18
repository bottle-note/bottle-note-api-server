package app.bottlenote.alcohols.domain;

/** 주류 삭제를 막는 참조를 확인하는 계약. 주류가 정하고 참조를 가진 업무가 구현한다. */
public interface AlcoholDeleteGuard {

  /** 거절 사유의 확인 순서. 값이 작을수록 먼저 확인하며 리뷰가 별점보다 앞선다. */
  int REVIEW_ORDER = 1;

  int RATING_ORDER = 2;

  /** 삭제를 막는 참조가 있으면 AlcoholException을 던진다. */
  void verifyDeletable(Long alcoholId);
}
