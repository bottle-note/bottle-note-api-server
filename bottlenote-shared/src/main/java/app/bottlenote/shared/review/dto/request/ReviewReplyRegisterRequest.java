package app.bottlenote.shared.review.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewReplyRegisterRequest(
    @NotBlank(message = "REQUIRED_REVIEW_REPLY_CONTENT")
        @Size(min = 1, max = 500, message = "CONTENT_IS_OUT_OF_RANGE")
        String content,
    Long parentReplyId) {}
