package app.bottlenote.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record NicknameChangeRequest(
    @NotBlank(message = "NICKNAME_NOT_BLANK")
        @Pattern(regexp = "^[a-zA-Z가-힣0-9]{2,11}$", message = "NICKNAME_PATTERN_NOT_VALID")
        String nickName) {}
