package app.bottlenote.global.security;

import static app.bottlenote.user.exception.UserExceptionCode.USER_NOT_FOUND;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.repository.OauthRepository;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialLoginAuthenticationProvider implements AuthenticationProvider {

	private final OauthRepository oauthRepository;

	@Override
	public Authentication authenticate(Authentication authentication)
		throws AuthenticationException {
		String email = authentication.getName();
		User user = oauthRepository.findByEmail(email)
			.orElseThrow(() -> new UserException(USER_NOT_FOUND));
		List<GrantedAuthority> authorities = Collections.singletonList(
			new SimpleGrantedAuthority("ROLE_USER"));
		return new UsernamePasswordAuthenticationToken(user.getEmail(), null, authorities);
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
	}
}

