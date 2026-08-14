package app.bottlenote.notification.dto.request;

import app.bottlenote.global.pagination.PaginationRequest;
import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationReadStatus;
import app.bottlenote.notification.constant.NotificationType;
import app.bottlenote.notification.dto.dsl.NotificationListCriteria;
import jakarta.validation.constraints.AssertTrue;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

public record NotificationPageableRequest(
    String cursor,
    Integer size,
    List<NotificationType> types,
    List<NotificationCategory> categories,
    NotificationReadStatus readStatus,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime createdFrom,
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime createdTo) {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  public static final int DEFAULT_SIZE = 10;
  public static final int MAX_SIZE = 100;

  @Builder
  public NotificationPageableRequest {
    PaginationRequest page = PaginationRequest.of(cursor, size, DEFAULT_SIZE, MAX_SIZE);
    cursor = page.cursor();
    size = page.size();
    types = types == null ? List.of() : types.stream().filter(Objects::nonNull).toList();
    categories =
        categories == null ? List.of() : categories.stream().filter(Objects::nonNull).toList();
    readStatus = readStatus != null ? readStatus : NotificationReadStatus.ALL;
  }

  @AssertTrue(message = "createdFrom은 createdTo보다 이전이어야 합니다.")
  public boolean isCreatedRangeValid() {
    return createdFrom == null || createdTo == null || createdFrom.isBefore(createdTo);
  }

  public NotificationListCriteria toCriteria(Long userId, Long lastId) {
    return new NotificationListCriteria(
        userId,
        lastId == null ? 0L : lastId,
        size.longValue(),
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
