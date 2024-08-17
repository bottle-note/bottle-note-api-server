package app.bottlenote.user.dto.request;

import app.bottlenote.user.domain.constant.GenderType;
import app.bottlenote.user.domain.constant.SocialType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OauthRequest
	(@NotBlank(message = "NOT_BLANK")
	 @Email(message = "EMAIL_PATTERN_NOT_VALID") String email,
	 @NotNull(message = "SOCIAL_TYPE_REQUIRED")
	 SocialType socialType,
	 GenderType gender,
	 @Min(value = 0, message = "AGE_MINIMUM")
	 Integer age
	) {

}
