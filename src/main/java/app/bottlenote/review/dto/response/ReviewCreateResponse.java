package app.bottlenote.review.dto.response;

import static app.bottlenote.review.exception.ReviewExceptionCode.INVALID_CALL_BACK_URL;

import app.bottlenote.review.exception.ReviewException;
import java.net.MalformedURLException;
import java.net.URL;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class ReviewCreateResponse {

	private final Long id;
	private final String content;
	private final URL callback;

	@Builder
	public ReviewCreateResponse(Long id, String content, String callback) {
		this.id = id;
		this.content = content;
		try {
			this.callback = new URL("https://bottle-note.com/api/v1/reviews/" + callback);
		} catch (MalformedURLException e) {
			throw new ReviewException(INVALID_CALL_BACK_URL);
		}
	}
}
