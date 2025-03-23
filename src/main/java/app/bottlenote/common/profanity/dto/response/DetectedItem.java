package app.bottlenote.common.profanity.dto.response;

public record DetectedItem(
	Integer length,
	String filteredWord
) {
	public static DetectedItem create(String filteredWord) {
		return new DetectedItem(filteredWord.length(), filteredWord);
	}
}
