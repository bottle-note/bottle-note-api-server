package app.bottlenote.support.help.dto.request;

import jakarta.validation.constraints.NotNull;

public record HelpImageInfo(
	@NotNull(message = "HELP_IMAGE_ORDER_REQUIRED")
	Long order,
	@NotNull(message = "HELP_IMAGE_URL_REQUIRED")
	String viewUrl
) {

	public static HelpImageInfo create(Long order, String viewUrl) {
		return new HelpImageInfo(order, viewUrl);
	}
}
