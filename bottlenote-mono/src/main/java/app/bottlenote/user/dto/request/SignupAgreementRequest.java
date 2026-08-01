package app.bottlenote.user.dto.request;

import app.bottlenote.user.constant.AgreementType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupAgreementRequest(
    @NotNull(message = "AGREEMENT_TYPE_REQUIRED") AgreementType type,
    @NotBlank(message = "AGREEMENT_VERSION_REQUIRED")
        @Size(max = 50, message = "AGREEMENT_VERSION_MAX_SIZE")
        String version) {}
