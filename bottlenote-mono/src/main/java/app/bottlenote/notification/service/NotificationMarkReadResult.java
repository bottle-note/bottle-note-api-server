package app.bottlenote.notification.service;

import java.time.LocalDateTime;

/**
 * 단건 알림의 멱등 읽음 처리 결과다.
 *
 * <p>최초 읽음 시각과 이번 요청의 변경 여부, 처리 후 미읽음 개수를 함께 전달한다.
 */
public record NotificationMarkReadResult(
    Long notificationId,
    boolean isRead,
    LocalDateTime readAt,
    boolean changed,
    long unreadCount) {}
