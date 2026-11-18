package app.bottlenote.user.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.FollowRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaFollowRepository extends FollowRepository, JpaRepository<Follow, Long> {

  @Query("SELECT f FROM follow f WHERE f.userId = :userId AND f.targetUserId = :followUserId")
  Optional<Follow> findByUserIdAndFollowUserId(
      @Param("userId") Long userId, @Param("followUserId") Long followUserId);
}
