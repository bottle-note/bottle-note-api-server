package app.bottlenote.support.help.dto.request;

import app.bottlenote.support.help.domain.constant.HelpType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HelpRegisterRequest(

	@NotEmpty(message = "제목은 Null이거나 공백일 수 없습니다.")
	String title,

	@NotEmpty(message = "문의 내용을 입력해주세요")
	@Size(max = 500)
	String content,

	@NotNull(message = "문의글 타입은 Null일 수 없습니다.")
	HelpType type
) {

}
