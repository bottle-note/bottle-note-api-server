package app.bottlenote.notification.dto.dsl;

import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationReadStatus;
import app.bottlenote.notification.constant.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 알림함 목록 조회 포트 기준(id-desc keyset). Spring/JPA 타입을 포함하지 않는다.
 *
 * <p>cursor 미지정/0이면 조건 없음. cursor &gt; 0이면 {@code id < cursor}.
 */
public record NotificationListCriteria(
    Long userId,
    Long cursor,
    Long pageSize,
    List<NotificationType> types,
    List<NotificationCategory> categories,
    NotificationReadStatus readStatus,
    LocalDateTime createdFrom,
    LocalDateTime createdTo) {

  public NotificationListCriteria {
    Objects.requireNonNull(userId, "userId는 필수입니다.");
    cursor = cursor != null ? cursor : 0L;
    pageSize = pageSize != null ? pageSize : 10L;
    types = types == null ? List.of() : types.stream().filter(Objects::nonNull).toList();
    categories =
        categories == null
            ? List.of()
            : categories.stream().filter(Objects::nonNull).toList();
    readStatus = readStatus != null ? readStatus : NotificationReadStatus.ALL;
  }

  public static NotificationListCriteria of(Long userId, Long cursor, Long pageSize) {
    return new NotificationListCriteria(
        userId,
        cursor,
        pageSize,
        List.of(),
        List.of(),
        NotificationReadStatus.ALL,
        null,
        null);
  }

  /** 다음 페이지 keyset 조건 사용 여부. 0 이하면 최초 페이지. */
  public boolean hasCursor() {
    return cursor != null && cursor > 0L;
  }

  /** hasNext 판별을 위해 pageSize + 1건을 조회한다. */
  public long fetchLimit() {
    return pageSize + 1;
  }
}
