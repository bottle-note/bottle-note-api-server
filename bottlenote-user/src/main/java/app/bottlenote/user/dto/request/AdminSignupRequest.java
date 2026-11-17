package app.bottlenote.user.dto.request;

import app.bottlenote.user.constant.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminSignupRequest(
    @NotBlank(message = "EMAIL_IS_REQUIRED") @Email(message = "USER_EMAIL_NOT_VALID") String email,
    @NotBlank(message = "PASSWORD_IS_REQUIRED")
        @Size(min = 8, max = 35, message = "USER_PASSWORD_NOT_VALID")
        String password,
    @NotBlank(message = "이름은 필수 입력값입니다.")
        @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하로 입력해주세요.")
        String name,
    @NotEmpty(message = "역할은 최소 1개 이상 선택해야 합니다.") List<AdminRole> roles) {}
