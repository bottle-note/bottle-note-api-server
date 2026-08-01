package app.bottlenote.user.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SignupRequest(
    @NotBlank(message = "SIGNUP_TOKEN_REQUIRED") String signupToken,
    @NotEmpty(message = "SIGNUP_AGREEMENTS_REQUIRED") @Valid
        List<SignupAgreementRequest> agreements) {}
