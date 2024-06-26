package app.bottlenote.common.profanity.response;

public record Detected(
	Integer length,
	String filteredWord
) {
	public static Detected create(String filteredWord) {
		return new Detected(filteredWord.length(), filteredWord);
	}
}
