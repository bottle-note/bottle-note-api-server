package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserRepository;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaUserRepository extends UserRepository, JpaRepository<User, Long>, CustomUserRepository {
	@Query("SELECT COUNT(u) > 0 FROM users u WHERE u.id = :id")
	Boolean existsByUserId(@Param("id") Long id);

	@Query("SELECT COUNT(u) FROM users u WHERE u.nickName like :nickName")
	Long countByUsername(@Param("nickName") String nickName);

	boolean existsByNickName(String nickname);

	Optional<User> findById(@NotNull Long id);
}
