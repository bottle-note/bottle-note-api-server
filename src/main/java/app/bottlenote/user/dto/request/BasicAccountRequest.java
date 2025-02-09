package app.bottlenote.user.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 해당 클래스는 사용자의 기본 계정 정보를 요청하는 DTO 클래스입니다.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BasicAccountRequest {
	@NotBlank(message = "EMAIL_IS_REQUIRED")
	private String email;
	@NotBlank(message = "PASSWORD_IS_REQUIRED")
	private String password;
	@Min(value = 0, message = "AGE_NEED_OVER_19")
	private Integer age;
	private String gender;
}
