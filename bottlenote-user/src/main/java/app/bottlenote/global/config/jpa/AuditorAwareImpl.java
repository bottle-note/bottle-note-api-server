package app.bottlenote.global.config.jpa;

import app.bottlenote.common.constant.AuditPrincipalType;
import app.bottlenote.common.domain.AuditPrincipal;
import app.bottlenote.global.security.CustomAdminUserContext;
import app.bottlenote.global.security.CustomUserContext;
import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** 인증 주체를 JPA 감사자로 공급한다. 사용자 인증 타입을 알기 때문에 최종 소유는 user 모듈이다. */
@Component
public class AuditorAwareImpl implements AuditorAware<AuditPrincipal> {

  @Override
  public Optional<AuditPrincipal> getCurrentAuditor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }

    Object principal = authentication.getPrincipal();
    if (principal instanceof CustomAdminUserContext admin) {
      return Optional.of(
          new AuditPrincipal(admin.getId(), AuditPrincipalType.ADMIN, admin.getUsername()));
    }
    if (principal instanceof CustomUserContext user) {
      AuditPrincipalType type =
          user.getId().equals(-4L) ? AuditPrincipalType.ANONYMOUS : AuditPrincipalType.USER;
      Long id = type == AuditPrincipalType.ANONYMOUS ? null : user.getId();
      return Optional.of(new AuditPrincipal(id, type, user.getUsername()));
    }
    return Optional.empty();
  }
}
