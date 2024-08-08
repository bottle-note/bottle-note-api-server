package app.bottlenote.support.help.dto.request;

import app.bottlenote.support.help.domain.constant.HelpType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record HelpRegisterRequest(

	@NotEmpty(message = "제목은 Null이거나 공백일 수 없습니다.")
	String title,

	@NotEmpty(message = "문의글 내용은 Null이거나 공백일 수 없습니다.")
	@Max(value = 1000, message = "문의글은 최대 1000자까지 작성할 수 있습니다.")
	String content,

	@NotNull(message = "문의글 타입은 Null일 수 없습니다.")
	HelpType type
) {

}
