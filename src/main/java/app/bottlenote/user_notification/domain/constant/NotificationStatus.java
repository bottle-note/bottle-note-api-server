package app.bottlenote.user_notification.domain.constant;

import lombok.Getter;

@Getter
public enum NotificationStatus {
	PENDING("대기 중"),
	SENT("전송됨"),
	READ("읽음"),
	FAILED("실패");

	private final String description;

	NotificationStatus(String description) {
		this.description = description;
	}

}
