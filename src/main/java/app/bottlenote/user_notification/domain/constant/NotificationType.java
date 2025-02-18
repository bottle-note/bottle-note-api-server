package app.bottlenote.user_notification.domain.constant;

import lombok.Getter;

@Getter
public enum NotificationType {
	SYSTEM("시스템 알림"),
	USER("사용자 알림"),
	PROMOTION("프로모션 알림");

	private final String description;

	NotificationType(String description) {
		this.description = description;
	}
}
