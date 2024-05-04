package app.bottlenote.global.security;

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
		log.info("Current Authentication: {}",
			SecurityContextHolder.getContext().getAuthentication());
		final Authentication authentication = SecurityContextHolder.getContext()
			.getAuthentication();

		User user = oauthRepository.findByEmail(authentication.getName())
			.orElseThrow(
				() -> new UserException(UserExceptionCode.USER_NOT_FOUND));
		log.info("user's email is : {}", user.getEmail());
		return user.getId();
	}

}
