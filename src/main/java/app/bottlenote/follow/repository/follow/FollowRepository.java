package app.bottlenote.follow.repository.follow;

import app.bottlenote.follow.domain.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long>, CustomFollowRepository {
	@Query("SELECT f FROM Follow f JOIN FETCH f.user JOIN FETCH f.followUser WHERE f.user.id = :userId AND f.followUser.id = :followUserId")
	Optional<Follow> findByUserIdAndFollowUserIdWithFetch(Long userId, Long followUserId);
}
