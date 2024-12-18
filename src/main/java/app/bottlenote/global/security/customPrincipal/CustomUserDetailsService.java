package app.bottlenote.global.security.customPrincipal;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.constant.UserType;
import app.bottlenote.user.repository.OauthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final OauthRepository oauthRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

		User customUser = oauthRepository.findByEmail(email)
			.orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority(customUser.getRole().toString()));

		return new CustomUserContext(customUser, authorities);
	}

	public UserDetails loadAnonymousUser() throws UsernameNotFoundException {
		User dAnonymousUser = User.builder()
			.id(-4L)
			.nickName("익명 사용자")
			.email("AnonymousUser@email.com")
			.build();
		List<GrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority(UserType.ROLE_ANONYMOUS.toString()));
		return new CustomUserContext(dAnonymousUser, authorities);
	}
}
