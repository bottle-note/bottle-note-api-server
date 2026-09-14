package app.bottlenote.like.repository;

import app.bottlenote.like.domain.Likes;
import app.bottlenote.like.domain.LikesRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaLikesRepository extends LikesRepository, JpaRepository<Likes, Long> {

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select l from likes l where l.reviewId = :reviewId and l.userInfo.userId = :userId")
  Optional<Likes> findForUpdateByReviewIdAndUserId(
      @Param("reviewId") Long reviewId, @Param("userId") Long userId);

  @Query(
      """
			select l
			from likes l
			where l.reviewId = :reviewId and l.userInfo.userId = :userId
			""")
  Optional<Likes> findByReviewIdAndUserId(
      @Param("reviewId") Long reviewId, @Param("userId") Long userId);
}
