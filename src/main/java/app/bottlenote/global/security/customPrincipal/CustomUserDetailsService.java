package app.bottlenote.global.security.customPrincipal;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.SocialType;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.OauthRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final OauthRepository oauthRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

		User customUser = oauthRepository.findByEmail(email)
			.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority(customUser.getRole().toString()));

		return new CustomUserContext(customUser, authorities);
	}

	public UserDetails loadUserByUsernameAndSocialType(String email, SocialType socialType) throws UsernameNotFoundException {

		User customUser = oauthRepository.findByEmailAndSocialType(email, socialType)
			.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority(customUser.getRole().toString()));

		return new CustomUserContext(customUser, authorities);
	}
}
