package app.bottlenote.notification.dto.request;

import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationReadStatus;
import app.bottlenote.notification.constant.NotificationType;
import app.bottlenote.notification.dto.dsl.NotificationListCriteria;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 알림함 커서 페이징 요청.
 *
 * <p>cursor는 직전 페이지 마지막 알림 id(keyset). 미지정/0이면 최신부터 조회한다.
 */
public record NotificationPageableRequest(
    @Min(0) Long cursor,
    @Min(1) @Max(100) Long pageSize,
    List<NotificationType> types,
    List<NotificationCategory> categories,
    NotificationReadStatus readStatus,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    OffsetDateTime createdFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    OffsetDateTime createdTo) {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  @Builder
  public NotificationPageableRequest {
    cursor = cursor != null ? cursor : 0L;
    pageSize = pageSize != null ? pageSize : 10L;
    types = types == null ? List.of() : types.stream().filter(Objects::nonNull).toList();
    categories =
        categories == null
            ? List.of()
            : categories.stream().filter(Objects::nonNull).toList();
    readStatus = readStatus != null ? readStatus : NotificationReadStatus.ALL;
  }

  @AssertTrue(message = "createdFrom은 createdTo보다 이전이어야 합니다.")
  public boolean isCreatedRangeValid() {
    return createdFrom == null || createdTo == null || createdFrom.isBefore(createdTo);
  }

  /**
   * API 요청 조건을 도메인 조회 포트가 사용하는 criteria로 변환한다.
   *
   * <p>오프셋 시각은 동일 instant의 KST 로컬 시각으로 정규화한다.
   */
  public NotificationListCriteria toCriteria(Long userId) {
    return new NotificationListCriteria(
        userId,
        cursor,
        pageSize,
        types,
        categories,
        readStatus,
        toKstLocalDateTime(createdFrom),
        toKstLocalDateTime(createdTo));
  }

  private static LocalDateTime toKstLocalDateTime(OffsetDateTime dateTime) {
    return dateTime != null ? dateTime.atZoneSameInstant(KST).toLocalDateTime() : null;
  }
}
