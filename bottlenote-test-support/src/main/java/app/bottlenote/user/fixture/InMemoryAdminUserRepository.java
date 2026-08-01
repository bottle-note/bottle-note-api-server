package app.bottlenote.user.fixture;

import app.bottlenote.user.constant.UserStatus;
import app.bottlenote.user.domain.AdminUser;
import app.bottlenote.user.domain.AdminUserRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryAdminUserRepository implements AdminUserRepository {

  private final Map<Long, AdminUser> adminUsers = new HashMap<>();
  private final AtomicLong sequence = new AtomicLong(0);

  @Override
  public AdminUser save(AdminUser adminUser) {
    if (adminUser.getId() == null) {
      ReflectionTestUtils.setField(adminUser, "id", sequence.incrementAndGet());
    }
    adminUsers.put(adminUser.getId(), adminUser);
    return adminUser;
  }

  @Override
  public Optional<AdminUser> findById(Long id) {
    return Optional.ofNullable(adminUsers.get(id));
  }

  @Override
  public Optional<AdminUser> findByEmail(String email) {
    return adminUsers.values().stream()
        .filter(admin -> admin.getEmail().equals(email))
        .findFirst();
  }

  @Override
  public Optional<AdminUser> findByRefreshToken(String refreshToken) {
    return adminUsers.values().stream()
        .filter(admin -> refreshToken.equals(admin.getRefreshToken()))
        .findFirst();
  }

  @Override
  public boolean existsByEmail(String email) {
    return adminUsers.values().stream().anyMatch(admin -> admin.getEmail().equals(email));
  }

  @Override
  public boolean existsActiveAdmin() {
    return adminUsers.values().stream()
        .anyMatch(admin -> admin.getStatus() == UserStatus.ACTIVE);
  }

  public void clear() {
    adminUsers.clear();
  }
}
