package app.bottlenote.notification.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

/**
 * 알림함 커서 페이징 요청.
 *
 * <p>cursor는 직전 페이지 마지막 알림 id(keyset). 미지정/0이면 최신부터 조회한다.
 */
public record NotificationPageableRequest(@Min(0) Long cursor, @Min(1) @Max(100) Long pageSize) {

  @Builder
  public NotificationPageableRequest {
    cursor = cursor != null ? cursor : 0L;
    pageSize = pageSize != null ? pageSize : 10L;
  }
}
