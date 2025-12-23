package app.bottlenote.user.repository;

import app.bottlenote.user.domain.AdminUser;
import app.bottlenote.user.domain.AdminUserRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAdminUserRepository
    extends AdminUserRepository, JpaRepository<AdminUser, Long> {

  Optional<AdminUser> findByEmail(String email);

  Optional<AdminUser> findByRefreshToken(String refreshToken);

  boolean existsByEmail(String email);

  @Query("SELECT COUNT(a) > 0 FROM admin_users a WHERE a.status = 'ACTIVE'")
  boolean existsActiveAdmin();
}
