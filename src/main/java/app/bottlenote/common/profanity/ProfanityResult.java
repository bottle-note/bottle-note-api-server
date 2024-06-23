package app.bottlenote.common.profanity;

public record ProfanityResult(
	String status,
	String message,
	boolean isProfane,
	String filteredText
) {
}
