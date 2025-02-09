package app.bottlenote.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
	@NotBlank(message = "이메일은 필수 입력 값입니다.")
	private String email;
	@NotBlank(message = "비밀번호는 필수 입력 값입니다.")
	private String password;
	@Size(min = 19, message = "나이는 19세 이상이어야 합니다.")
	private Integer age;
	private String gender;
}
