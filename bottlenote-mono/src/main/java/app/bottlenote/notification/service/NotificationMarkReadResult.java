package app.bottlenote.notification.service;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/** 단건 알림 읽음 처리 결과. */
public record NotificationMarkReadResult(
    Long notificationId,
    boolean isRead,
    @Nullable LocalDateTime readAt,
    boolean changed,
    long unreadCount) {}
