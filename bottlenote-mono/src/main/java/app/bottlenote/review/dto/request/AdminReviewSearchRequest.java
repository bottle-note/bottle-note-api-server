package app.bottlenote.review.dto.request;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.constant.AdminReviewSortType;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import java.time.LocalDateTime;

public record AdminReviewSearchRequest(
    Long alcoholId,
    Long userId,
    ReviewActiveStatus activeStatus,
    ReviewDisplayStatus displayStatus,
    String keyword,
    LocalDateTime createdFrom,
    LocalDateTime createdTo,
    AdminReviewSortType sortType,
    SortOrder sortOrder,
    Integer page,
    Integer size) {

  public AdminReviewSearchRequest {
    sortType = sortType != null ? sortType : AdminReviewSortType.CREATED_AT;
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
    page = page != null ? page : 0;
    size = size != null ? size : 20;
  }
}
