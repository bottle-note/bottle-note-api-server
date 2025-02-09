package app.bottlenote.user.dto.request;

import lombok.Getter;

/**
 * 해당 클래스는 사용자의 기본 계정 정보를 요청하는 DTO 클래스입니다.
 */
@Getter
public class BasicAccountRequest {
	private String loginId;
	private String password;
}
