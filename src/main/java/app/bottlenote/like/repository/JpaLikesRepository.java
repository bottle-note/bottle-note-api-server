package app.bottlenote.like.repository;

import app.bottlenote.like.domain.Likes;
import app.bottlenote.like.domain.LikesRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaLikesRepository extends LikesRepository, JpaRepository<Likes, Long> {

	@Query(
		"""
			select l
			from likes l
			join fetch l.reviewId
			where l.reviewId = :reviewId and l.userInfo.userId = :userId
			""")
	Optional<Likes> findByReviewIdAndUserId(@Param("reviewId") Long reviewId, @Param("userId") Long userId);
}
