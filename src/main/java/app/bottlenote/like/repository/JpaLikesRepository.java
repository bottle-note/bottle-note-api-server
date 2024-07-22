package app.bottlenote.like.repository;

import app.bottlenote.like.domain.Likes;
import app.bottlenote.like.domain.LikesRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface JpaLikesRepository extends LikesRepository, JpaRepository<Likes, Long> {

///@Query("SELECT r FROM user_report r JOIN FETCH r.user JOIN FETCH r.reportUser WHERE r.user.id = :user_id AND r.createAt >= :date")
	@Query(
		"select l " +
                "from likes l " +
                "join fetch l.review " +
                "where l.review.id = :reviewId " +
                "and l.userInfo.userId = :userId"
	)
	@Override
	Optional<Likes> findByReviewIdAndUserId(Long reviewId, Long userId);
}
