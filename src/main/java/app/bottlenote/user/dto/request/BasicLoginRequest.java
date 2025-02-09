package app.bottlenote.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BasicLoginRequest {
	@NotBlank(message = "이메일을 입력해주세요.")
	private String email;
	@NotBlank(message = "비밀번호를 입력해주세요.")
	@Size(min = 8, max = 35, message = "비밀번호는 8자 이상 35자 이하로 입력해주세요.")
	private String password;
}
