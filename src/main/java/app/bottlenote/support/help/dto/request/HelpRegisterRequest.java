package app.bottlenote.support.help.dto.request;

import app.bottlenote.support.help.domain.constant.HelpType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HelpRegisterRequest(

	@NotEmpty(message = "TITLE_NOT_EMPTY")
	String title,

	@NotEmpty(message = "CONTENT_NOT_EMPTY")
	@Size(max = 500)
	String content,

	@NotNull(message = "REQUIRED_HELP_TYPE")
	HelpType type

) {

}
