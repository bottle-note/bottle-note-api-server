package app.bottlenote.support.help.dto.request;

import app.bottlenote.support.constant.StatusType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminHelpAnswerRequest(
    @NotBlank(message = "답변 내용은 필수입니다.") String responseContent,
    @NotNull(message = "처리 상태는 필수입니다.") StatusType status) {}
