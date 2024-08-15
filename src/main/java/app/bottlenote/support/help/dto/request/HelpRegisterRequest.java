package app.bottlenote.support.help.dto.request;

import app.bottlenote.support.help.domain.constant.HelpType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HelpRegisterRequest(

	@NotEmpty(message = "HELP_TITLE_REQUIRED")
	String title,

	@NotEmpty(message = "HELP_CONTENT_REQUIRED")
	@Size(max = 500)
	String content,

	@NotNull(message = "VALUE_REQUIRED")
	HelpType type
) {

}
