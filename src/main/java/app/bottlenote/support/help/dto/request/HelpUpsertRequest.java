package app.bottlenote.support.help.dto.request;

import app.bottlenote.support.help.domain.constant.HelpType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record HelpUpsertRequest(

	@NotEmpty(message = "CONTENT_NOT_EMPTY")
	@Size(max = 500)
	String content,

	@NotNull(message = "REQUIRED_HELP_TYPE")
	HelpType type,

	@Valid
	List<HelpImageItem> imageUrlList
) {
	public HelpUpsertRequest {
		imageUrlList = imageUrlList == null ? List.of() : imageUrlList;
	}
}
