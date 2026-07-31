package app.bottlenote.user.domain;

import java.util.Optional;

public interface AdminUserRepository {

  AdminUser save(AdminUser adminUser);

  Optional<AdminUser> findById(Long id);

  Optional<AdminUser> findByEmail(String email);

  Optional<AdminUser> findByRefreshToken(String refreshToken);

  Optional<AdminUser> findByAgentId(String agentId);

  boolean existsByEmail(String email);

  boolean existsActiveAdmin();
}
