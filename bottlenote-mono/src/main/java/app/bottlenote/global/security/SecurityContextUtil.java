package app.bottlenote.global.security;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SecurityContextUtil {

  private static final String contextNotFount = "보안 컨텍스트에 인증 정보가 없습니다. 사용자가 로그인하지 않았을 수 있습니다.";

  /**
   * 기존 버전에서 사용중인 메서드입니다. getUserIdByContext() 메서드로 대체하여 사용하도록 권고합니다.
   *
   * @return the current user id
   */
  @Deprecated
  public static Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Long userId = null;

    if (authentication != null
        && authentication.getPrincipal() instanceof CustomUserContext customUserContext) {
      log.warn(
          contextNotFount
              + "/ getCurrentUserId() 메서드는 사용 중단되었습니다. getUserIdByContext() 메서드를 사용하도록 권고합니다.");
      userId = customUserContext.getId();
    }
    return userId;
  }

  /**
   * 현재 세션에서 인증된 사용자의 ID를 반환합니다. 기존 버전에서 옵셔널을 사용하도록 변경하였습니다.
   *
   * @return the user id by context
   */
  public static Optional<Long> getUserIdByContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
      log.debug(contextNotFount);
      return Optional.empty();
    }

    Object principal = authentication.getPrincipal();

    // 테스트 환경에서 TestingAuthenticationToken 지원
    if (authentication.getClass().getSimpleName().equals("TestingAuthenticationToken")) {
      try {
        Long userId = Long.parseLong(principal.toString());
        log.debug("테스트 환경에서 사용자 ID 추출: {}", userId);
        return Optional.of(userId);
      } catch (NumberFormatException e) {
        log.warn("테스트 환경에서 사용자 ID 파싱 실패: {}", principal);
        return Optional.empty();
      }
    }
    if (!(principal instanceof CustomUserContext customUserContext)) {
      log.debug("인증된 사용자의 정보가 CustomUserContext의 인스턴스가 아닙니다. 비회원 사용자일 수 있습니다.");
      return Optional.empty();
    }

    Long id = customUserContext.getId();
    if (id == null || id.equals(-4L)) {
      log.debug("익명 사용자 접근: SecurityContextUtil.getUserIdByContext()");
      return Optional.empty();
    }

    return Optional.of(id);
  }

  /**
   * 현재 세션에서 인증된 어드민 사용자의 ID를 반환합니다.
   *
   * @return the admin user id by context
   */
  public static Optional<Long> getAdminUserIdByContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null) {
      log.debug(contextNotFount);
      return Optional.empty();
    }

    Object principal = authentication.getPrincipal();

    if (!(principal instanceof CustomAdminUserContext adminContext)) {
      log.debug("인증된 사용자의 정보가 CustomAdminUserContext의 인스턴스가 아닙니다.");
      return Optional.empty();
    }

    Long id = adminContext.getId();
    if (id == null) {
      log.debug("어드민 사용자 ID가 null입니다.");
      return Optional.empty();
    }

    return Optional.of(id);
  }
}
