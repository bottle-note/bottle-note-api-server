package app.bottlenote.global.config.jpa;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class AuditorAwareImpl implements AuditorAware<String> {

	@Override
	public Optional<String> getCurrentAuditor() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication != null && authentication.isAuthenticated()) {
			Object principal = authentication.getPrincipal();

			// principal이 UserDetails 타입인지 확인
			if (principal instanceof UserDetails) {
				UserDetails userDetails = (UserDetails) principal;
				return Optional.ofNullable(userDetails.getUsername());
			}
			// principal이 String 타입일 경우 직접 반환
			else if (principal instanceof String) {
				return Optional.of((String) principal);
			}
		}
		return Optional.empty();
	}
}
