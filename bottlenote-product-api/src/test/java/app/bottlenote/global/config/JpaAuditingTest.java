package app.bottlenote.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.common.constant.AuditPrincipalType;
import app.bottlenote.common.domain.AuditPrincipal;
import app.bottlenote.global.config.jpa.AuditorAwareImpl;
import app.bottlenote.global.security.CustomAdminUserContext;
import app.bottlenote.global.security.CustomUserContext;
import app.bottlenote.user.constant.AdminRole;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.AdminUser;
import app.bottlenote.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@Tag("unit")
@DisplayName("[unit] [infra] JpaAuditing")
class JpaAuditingTest {

  private final AuditorAwareImpl auditorAware = new AuditorAwareImpl();

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("사용자로 인증할 때 사용자 principal을 감사 주체로 반환한다")
  void 사용자로_인증할_때_사용자_principal을_반환한다() {
    User user =
        User.builder()
            .id(1L)
            .email("user@example.com")
            .nickName("user")
            .role(UserType.ROLE_USER)
            .build();
    CustomUserContext principal =
        new CustomUserContext(user, List.of(new SimpleGrantedAuthority(UserType.ROLE_USER.name())));
    authenticate(principal);

    Optional<AuditPrincipal> result = auditorAware.getCurrentAuditor();

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getId()).isEqualTo(1L);
    assertThat(result.orElseThrow().getType()).isEqualTo(AuditPrincipalType.USER);
    assertThat(result.orElseThrow().getEmail()).isEqualTo("user@example.com");
  }

  @Test
  @DisplayName("관리자로 인증할 때 관리자 principal을 감사 주체로 반환한다")
  void 관리자로_인증할_때_관리자_principal을_반환한다() {
    AdminUser admin =
        AdminUser.builder()
            .id(2L)
            .email("admin@example.com")
            .password("encoded")
            .name("admin")
            .roles(List.of(AdminRole.ROOT_ADMIN))
            .build();
    CustomAdminUserContext principal =
        new CustomAdminUserContext(
            admin, List.of(new SimpleGrantedAuthority(AdminRole.ROOT_ADMIN.name())));
    authenticate(principal);

    Optional<AuditPrincipal> result = auditorAware.getCurrentAuditor();

    assertThat(result).isPresent();
    assertThat(result.orElseThrow().getId()).isEqualTo(2L);
    assertThat(result.orElseThrow().getType()).isEqualTo(AuditPrincipalType.ADMIN);
    assertThat(result.orElseThrow().getEmail()).isEqualTo("admin@example.com");
  }

  @Test
  @DisplayName("인증되지 않았을 때 감사 주체를 반환하지 않는다")
  void 인증되지_않았을_때_감사_주체를_반환하지_않는다() {
    assertThat(auditorAware.getCurrentAuditor()).isEmpty();
  }

  private void authenticate(UserDetails principal) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()));
  }
}
