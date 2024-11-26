package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserQueryRepository;
import app.bottlenote.user.repository.custom.CustomUserRepository;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface JpaUserQueryRepository extends UserQueryRepository, JpaRepository<User, Long>, CustomUserRepository {

	@Query("SELECT COUNT(u) > 0 FROM users u WHERE u.id = :id")
	Boolean existsByUserId(@Param("id") Long id);

	@Query("SELECT COUNT(u) FROM users u WHERE u.nickName like :nickName")
	Long countByUsername(@Param("nickName") String nickName);

	Optional<User> findById(Long id);

	Boolean existsByNickName(String nickName);

}
