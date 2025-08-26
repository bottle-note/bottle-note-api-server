package app.bottlenote.user.dto.request;

import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OauthRequest(
    @Email(message = "EMAIL_PATTERN_NOT_VALID") String email,
    String socialUniqueId,
    @NotNull(message = "SOCIAL_TYPE_REQUIRED") SocialType socialType,
    GenderType gender,
    @Min(value = 0, message = "AGE_MINIMUM") Integer age) {}
