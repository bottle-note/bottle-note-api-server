package app.external.push.dto.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TokenMessage {
	DEVICE_TOKEN_REQUIRED("디바이스 토큰이 필요합니다."),
	DEVICE_TOKEN_SAVED("디바이스 토큰이 저장되었습니다."),
	DEVICE_TOKEN_DELETED("디바이스 토큰이 삭제되었습니다."),
	DEVICE_TOKEN_NOT_FOUND("디바이스 토큰을 찾을 수 없습니다.");

	private final String message;
}
