package app.bottlenote.user.fixture;

import app.bottlenote.user.constant.AdminRole;
import app.bottlenote.user.constant.UserStatus;
import app.bottlenote.user.domain.AdminUser;
import jakarta.persistence.EntityManager;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class AdminUserTestFactory {

  private final Random random = new SecureRandom();

  @Autowired private EntityManager em;

  @Autowired private BCryptPasswordEncoder passwordEncoder;

  private String generateRandomSuffix() {
    return String.valueOf(random.nextInt(10000));
  }

  /** 기본 ROOT_ADMIN 생성 */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @NotNull
  public AdminUser persistRootAdmin() {
    return persistAdminUser(
        "root-" + generateRandomSuffix() + "@bottlenote.com",
        "password123",
        "ROOT_ADMIN",
        List.of(AdminRole.ROOT_ADMIN));
  }

  /** 특정 이메일로 ROOT_ADMIN 생성 */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @NotNull
  public AdminUser persistRootAdmin(@NotNull String email, @NotNull String rawPassword) {
    return persistAdminUser(email, rawPassword, "ROOT_ADMIN", List.of(AdminRole.ROOT_ADMIN));
  }

  /** PARTNER 역할 어드민 생성 */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @NotNull
  public AdminUser persistPartnerAdmin() {
    return persistAdminUser(
        "partner-" + generateRandomSuffix() + "@bottlenote.com",
        "password123",
        "PARTNER_ADMIN",
        List.of(AdminRole.PARTNER));
  }

  /** 다중 역할 어드민 생성 */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @NotNull
  public AdminUser persistMultiRoleAdmin(@NotNull List<AdminRole> roles) {
    return persistAdminUser(
        "multi-" + generateRandomSuffix() + "@bottlenote.com",
        "password123",
        "MULTI_ROLE_ADMIN",
        roles);
  }

  /** 커스텀 어드민 생성 */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @NotNull
  public AdminUser persistAdminUser(
      @NotNull String email,
      @NotNull String rawPassword,
      @NotNull String name,
      @NotNull List<AdminRole> roles) {
    AdminUser adminUser =
        AdminUser.builder()
            .email(email)
            .password(passwordEncoder.encode(rawPassword))
            .name(name)
            .roles(roles)
            .status(UserStatus.ACTIVE)
            .build();
    em.persist(adminUser);
    em.flush();
    return adminUser;
  }

  /** 비활성 어드민 생성 */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @NotNull
  public AdminUser persistInactiveAdmin(@NotNull String email, @NotNull String rawPassword) {
    AdminUser adminUser =
        AdminUser.builder()
            .email(email)
            .password(passwordEncoder.encode(rawPassword))
            .name("INACTIVE_ADMIN")
            .roles(List.of(AdminRole.PARTNER))
            .status(UserStatus.DELETED)
            .build();
    em.persist(adminUser);
    em.flush();
    return adminUser;
  }

  /** 빌더 기반 어드민 생성 (비밀번호 자동 인코딩) */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @NotNull
  public AdminUser persistAdminUser(
      @NotNull AdminUser.AdminUserBuilder builder, @NotNull String rawPassword) {
    AdminUser adminUser = builder.password(passwordEncoder.encode(rawPassword)).build();
    em.persist(adminUser);
    em.flush();
    return adminUser;
  }
}
