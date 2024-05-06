package app.bottlenote.global.security;

import app.bottlenote.global.security.customPrincipal.CustomUserContext;
import app.bottlenote.user.repository.OauthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtil {

	private final OauthRepository oauthRepository;

	public static Long getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Long userId = null;

		if (authentication instanceof UsernamePasswordAuthenticationToken) {
			CustomUserContext customUserContext = (CustomUserContext) authentication.getPrincipal();
			userId = customUserContext.getId();
		}

		return userId;
	}

}
