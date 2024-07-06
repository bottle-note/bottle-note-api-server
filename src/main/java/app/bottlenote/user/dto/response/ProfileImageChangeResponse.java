package app.bottlenote.user.dto.response;

import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import lombok.Builder;
import lombok.Getter;

import java.net.URL;

@Getter
public class ProfileImageChangeResponse {

	private final Long userId;
	private final String profileImageUrl;
	private final URL callback;

	@Builder
	public ProfileImageChangeResponse(Long userId, String profileImageUrl, String callback) {
		this.userId = userId;
		this.profileImageUrl = profileImageUrl;
		try {
			this.callback = new URL("https://bottle-note.com/api/v1/users/" + userId);
		} catch (Exception e) {
			throw new UserException(UserExceptionCode.INVALID_CALL_BACK_URL);
		}
	}
}
