package app.bottlenote.global.security;

import app.bottlenote.global.security.customPrincipal.CustomUserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SecurityUtil {

	public static Long getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		Long userId = null;


		if (authentication != null && authentication.getPrincipal() instanceof CustomUserContext customUserContext) {
			userId = customUserContext.getId();
		}

		return userId;
	}
}
