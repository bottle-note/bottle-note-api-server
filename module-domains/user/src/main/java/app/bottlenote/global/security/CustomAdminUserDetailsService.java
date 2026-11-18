package app.bottlenote.global.security;

import app.bottlenote.user.constant.AdminRole;
import app.bottlenote.user.constant.UserStatus;
import app.bottlenote.user.domain.AdminUser;
import app.bottlenote.user.domain.AdminUserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CustomAdminUserDetailsService implements UserDetailsService {

  private final AdminUserRepository adminUserRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    AdminUser adminUser =
        adminUserRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("관리자를 찾을 수 없습니다: " + email));

    if (adminUser.getStatus() != UserStatus.ACTIVE) {
      throw new UsernameNotFoundException("비활성화된 관리자 계정입니다: " + email);
    }

    List<GrantedAuthority> authorities =
        adminUser.getRoles().stream()
            .map(AdminRole::name)
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());

    return new CustomAdminUserContext(adminUser, authorities);
  }
}
