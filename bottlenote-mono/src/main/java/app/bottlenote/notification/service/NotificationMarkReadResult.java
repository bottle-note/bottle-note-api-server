package app.bottlenote.notification.service;

import java.time.LocalDateTime;

/** 단건 알림 읽음 처리 결과. */
public record NotificationMarkReadResult(
    Long notificationId,
    boolean isRead,
    LocalDateTime readAt,
    boolean changed,
    long unreadCount) {}
