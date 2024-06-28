package app.bottlenote.common.profanity.request;

import java.util.Objects;

public record ProfanityRequest(
	String text,
	FilterMode mode
) {
	public static ProfanityRequest createFilter(String text) {
		Objects.requireNonNull(text, "text must be provided");
		return new ProfanityRequest(text, FilterMode.FILTER);
	}
}
