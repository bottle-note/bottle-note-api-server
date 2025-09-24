package app.bottlenote.review.repository;

import app.bottlenote.review.dto.request.ReviewPageableRequest;
import app.bottlenote.review.dto.response.ReviewExploreItem;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.shared.cursor.CursorResponse;
import app.bottlenote.shared.cursor.PageResponse;
import app.bottlenote.shared.review.payload.ReviewInfo;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public interface CustomReviewRepository {

  /***
   * 특정 리뷰에 대한 리뷰 상세 정보(댓글 포함)를 조회합니다.
   *
   * @param reviewId 조회 대상 리뷰 ID
   * @param userId 조회하는 사용자 ID
   * @return 리뷰 상세 정보
   */
  ReviewInfo getReview(Long reviewId, Long userId);

  /**
   * 특정 술에 대한 전체 리뷰 목록을 조회합니다
   *
   * @param alcoholId 조회 대상 알코올 ID
   * @param reviewPageableRequest 정렬기준, 페이징 처리 기준
   * @param userId 조회하는 사용자 ID
   * @return 특정 술에 대한 전체 리뷰 목록
   */
  PageResponse<ReviewListResponse> getReviews(
      Long alcoholId, ReviewPageableRequest reviewPageableRequest, Long userId);

  /**
   * 특정 술에 대해 로그인한 사용자가 작성한 리뷰 목록을 조회합니다.
   *
   * @param alcoholId 조회 대상 알코올 ID
   * @param reviewPageableRequest 정렬기준, 페이징 처리 기준
   * @param userId 조회하는 사용자 ID
   * @return 특정 술에 대한 내가 작성한 리뷰 목록
   */
  PageResponse<ReviewListResponse> getReviewsByMe(
      Long alcoholId, ReviewPageableRequest reviewPageableRequest, Long userId);

  Pair<Long, CursorResponse<ReviewExploreItem>> getStandardExplore(
      Long userId, List<String> keywords, Long cursor, Integer size);
}
