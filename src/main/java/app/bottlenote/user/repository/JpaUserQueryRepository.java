package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserQueryRepository;
import app.bottlenote.user.repository.custom.CustomUserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaUserQueryRepository extends UserQueryRepository, JpaRepository<User, Long>, CustomUserRepository {

	@Query("SELECT COUNT(u) > 0 FROM users u WHERE u.id = :id")
	Boolean existsByUserId(Long id);

	@Query("SELECT COUNT(u) FROM users u WHERE u.nickName like :nickName")
	Long countByUsername(String nickName);
	
}
