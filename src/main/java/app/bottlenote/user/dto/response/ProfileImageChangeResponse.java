package app.bottlenote.user.dto.response;

import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import java.net.MalformedURLException;
import java.net.URL;

public record ProfileImageChangeResponse(
	Long userId,
	String profileImageUrl,
	URL callback
) {

	public ProfileImageChangeResponse(Long userId, String profileImageUrl) {
		this(userId, profileImageUrl, createCallbackUrl(userId));
	}

	private static URL createCallbackUrl(Long userId) {
		try {
			return new URL("https://bottle-note.com/api/v1/users/" + userId);
		} catch (MalformedURLException e) {
			throw new UserException(UserExceptionCode.INVALID_CALL_BACK_URL);
		}
	}
}
