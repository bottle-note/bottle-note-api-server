package app.bottlenote.review.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewReplyRegisterRequest(
	@NotBlank(message = "댓글 내용은 필수 입력값입니다.")
	@Size(min = 1, max = 500, message = "댓글 내용은 1자 이상 500자 이하로 작성해주세요.")
	String content,
	Long parentReplyId
) {
}
