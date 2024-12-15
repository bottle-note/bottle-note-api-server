package app.bottlenote.user.repository;

import app.bottlenote.user.domain.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long>, CustomFollowRepository {
	@Query("SELECT f FROM Follow f JOIN FETCH f.followUser WHERE f.userId = :userId AND f.followUser.id = :followUserId")
	Optional<Follow> findByUserIdAndFollowUserIdWithFetch(@Param("userId") Long userId, @Param("followUserId") Long followUserId);
}
