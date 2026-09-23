package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUserRepository
    extends UserRepository, JpaRepository<User, Long>, CustomUserRepository {

  @Query("SELECT COUNT(u) > 0 FROM users u WHERE u.id = :userId")
  boolean existsByUserId(@Param("userId") Long userId);

  boolean existsByNickName(String nickname);

  Optional<User> findById(@NotNull Long id);

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from users u where u.id = :userId")
  Optional<User> findByIdForUpdate(@Param("userId") Long userId);
}
