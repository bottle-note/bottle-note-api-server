package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.MypageResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	@Query("SELECT new app.bottlenote.user.dto.response.MypageResponse(u.id, u.nickName, u.imageUrl, " +
		"(SELECT COUNT(f) FROM follow f WHERE f.user.id = :userId), " +
		"(SELECT COUNT(f) FROM follow f WHERE f.followUser.id = :userId), " +
		"(SELECT COUNT(l) FROM likes l WHERE l.userInfo.userId = :userId), " +
		"(SELECT COUNT(r) FROM review r WHERE r.userId = :userId), " +
		"(SELECT COUNT(rt) FROM rating rt WHERE rt.user.id = :userId), " +
		"CASE WHEN :currentUserId = u.id THEN TRUE ELSE FALSE END, " +
		"(SELECT COUNT(f) > 0 FROM follow f WHERE f.user.id = :currentUserId AND f.followUser.id = :userId)) " +
		"FROM users u " +
		"LEFT JOIN FETCH u.following " +
		"LEFT JOIN FETCH u.followers " +
		"LEFT JOIN FETCH u.likes " +
		"LEFT JOIN FETCH u.reviews " +
		"LEFT JOIN FETCH u.ratings " +
		"WHERE u.id = :userId")
	MypageResponse getMypageData(@Param("userId") Long userId, @Param("currentUserId") Long currentUserId);

	@Query("SELECT COUNT(f) > 0 FROM follow f WHERE f.id = :currentUserId AND f.followUser.id = :userId")
	boolean isFollowing(@Param("currentUserId") Long currentUserId, @Param("userId") Long userId);
}
