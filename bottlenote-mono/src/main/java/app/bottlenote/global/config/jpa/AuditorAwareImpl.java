package app.bottlenote.global.config.jpa;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

@Slf4j
public class AuditorAwareImpl implements AuditorAware<String> {

  @Override
  public Optional<String> getCurrentAuditor() {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && authentication.isAuthenticated()) {
      if ("[ROLE_ANONYMOUS]".equals(authentication.getAuthorities().toString())) {
        return Optional.of("anonymousUser");
      }
      UserDetails userDetails = (UserDetails) authentication.getPrincipal();
      return Optional.ofNullable(userDetails.getUsername());
    }
    return Optional.empty();
  }
}
