package app.bottlenote.rating.dto.response;

import app.bottlenote.rating.domain.Rating;
import lombok.Getter;

public record RatingRegisterResponse(
	String rating,
	String message
) {

	public static RatingRegisterResponse success(Rating rating) {
		return new RatingRegisterResponse(
			rating.getRatingPoint().getRating().toString(),
			Message.SUCCESS.getMessage()
		);
	}

	public static RatingRegisterResponse fail() {
		return new RatingRegisterResponse(null, Message.FAIL.getMessage());
	}

	@Getter
	public enum Message {
		SUCCESS("별점 등록 성공"),
		FAIL("별점 등록 실패");

		private final String message;

		Message(String message) {
			this.message = message;
		}

	}
}
