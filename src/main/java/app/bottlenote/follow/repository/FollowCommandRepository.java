package app.bottlenote.follow.repository;

import app.bottlenote.follow.domain.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FollowCommandRepository extends JpaRepository<Follow, Long> {
	Optional<Follow> findByFollowUser(Long userId, Long followUserId);
}
