package app.bottlenote.review.dto.request;

import app.bottlenote.global.service.cursor.SortOrder;
import app.bottlenote.review.constant.AdminReviewSortType;
import app.bottlenote.review.constant.ReviewActiveStatus;
import app.bottlenote.review.constant.ReviewDisplayStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public record AdminReviewSearchRequest(
    Long alcoholId,
    Long userId,
    ReviewActiveStatus activeStatus,
    ReviewDisplayStatus displayStatus,
    String keyword,
    // 관리자 화면은 날짜만 보낸다. 기존 ISO 날짜·시각 값(초·소수 초 포함)도 그 날짜로 받아 하루 단위로 거른다.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE, fallbackPatterns = LOCAL_DATE_TIME_PATTERN)
        LocalDate createdFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE, fallbackPatterns = LOCAL_DATE_TIME_PATTERN)
        LocalDate createdTo,
    AdminReviewSortType sortType,
    SortOrder sortOrder,
    @Min(value = 0, message = "ADMIN_REVIEW_PAGE_MINIMUM") Integer page,
    @Min(value = 1, message = "ADMIN_REVIEW_SIZE_MINIMUM")
        @Max(value = 100, message = "ADMIN_REVIEW_SIZE_MAXIMUM")
        Integer size) {

  /** 분까지, 초까지, 그리고 밀리초·마이크로초·나노초까지의 ISO 날짜·시각을 받는다. offset이 붙은 값은 받지 않는다. */
  public static final String LOCAL_DATE_TIME_PATTERN =
      "yyyy-MM-dd'T'HH:mm[:ss][.SSSSSSSSS][.SSSSSS][.SSS]";

  public AdminReviewSearchRequest {
    sortType = sortType != null ? sortType : AdminReviewSortType.CREATED_AT;
    sortOrder = sortOrder != null ? sortOrder : SortOrder.DESC;
    page = page != null ? page : 0;
    size = size != null ? size : 20;
  }
}
