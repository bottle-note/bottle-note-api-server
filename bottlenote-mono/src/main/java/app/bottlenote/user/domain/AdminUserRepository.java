package app.bottlenote.user.domain;

import java.util.Optional;

public interface AdminUserRepository {

  AdminUser save(AdminUser adminUser);

  Optional<AdminUser> findById(Long id);

  Optional<AdminUser> findByEmail(String email);

  boolean existsByEmail(String email);
}
