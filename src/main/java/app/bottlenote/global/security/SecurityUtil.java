package app.bottlenote.global.security;

import static io.jsonwebtoken.lang.Strings.hasText;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.OauthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtil {

	private final OauthRepository oauthRepository;

	public Long getCurrentUserId() {

		final Authentication authentication = SecurityContextHolder.getContext()
			.getAuthentication();

		if (authentication == null || !hasText(authentication.getName()) || authentication.getName()
			.equals("anonymousUser")) {
			throw new UserException(UserExceptionCode.AUTHORIZE_INFO_NOT_FOUND);
		}
		User user = oauthRepository.findByEmail(authentication.getName()).orElseThrow(
			() -> new UserException(UserExceptionCode.USER_NOT_FOUND));
		return user.getId();
	}

}
