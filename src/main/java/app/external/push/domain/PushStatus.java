package app.external.push.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PushStatus {
	PENDING("대기중", "푸시 메시지가 대기 중입니다."),
	DELIVERED("전송 완료", "푸시 메시지가 전송되었습니다."),
	FAILED("전송 실패", "푸시 메시지 전송에 실패했습니다."),
	CANCELED("전송 취소", "푸시 메시지 전송이 취소되었습니다.");

	private final String status;
	private final String description;
}
