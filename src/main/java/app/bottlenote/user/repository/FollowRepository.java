package app.bottlenote.user.repository;

import app.bottlenote.alcohols.dto.response.detail.FriendInfo;
import app.bottlenote.user.domain.Follow;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long>, CustomFollowRepository {
	@Query("SELECT f FROM Follow f JOIN FETCH f.followUser WHERE f.userId = :userId AND f.followUser.id = :followUserId")
	Optional<Follow> findByUserIdAndFollowUserIdWithFetch(@Param("userId") Long userId, @Param("followUserId") Long followUserId);

	@Query("""
		    SELECT new app.bottlenote.alcohols.dto.response.detail.FriendInfo(
		        u.imageUrl,
		        u.id,
		        u.nickName,
		        r.ratingPoint.rating
		    )
		    FROM users u
		    JOIN rating r ON r.user.id = u.id
		    WHERE u.id IN (
		        SELECT f.followUser.id
		        FROM Follow f
		        WHERE f.userId = :userId
		    )
		    AND r.alcohol.id = :alcoholId
		""")
	List<FriendInfo> getTastingFriendsInfoList(@Param("alcoholId") Long alcoholId, @Param("userId") Long userId, PageRequest pageRequest);
}
