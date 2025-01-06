package app.bottlenote.user.repository;

import app.bottlenote.alcohols.dto.response.detail.FriendInfo;
import app.bottlenote.user.domain.Follow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long>, CustomFollowRepository {

	@Query("SELECT f FROM Follow f WHERE f.userId = :userId AND f.targetUserId = :followUserId")
	Optional<Follow> findByUserIdAndFollowUserId(@Param("userId") Long userId, @Param("followUserId") Long followUserId);

	@Query("""
		    SELECT new app.bottlenote.alcohols.dto.response.detail.FriendInfo(
		        u.imageUrl,
		        u.id,
		        u.nickName,
		        r.ratingPoint.rating
		    )
		    FROM users u
		    JOIN rating r ON r.id.userId = u.id
		    WHERE u.id IN (
		        SELECT f.targetUserId
		        FROM Follow f
		        WHERE f.userId = :userId
		    )
		    AND r.id.alcoholId = :alcoholId
		""")
	List<FriendInfo> getTastingFriendsInfoList(@Param("alcoholId") Long alcoholId, @Param("userId") Long userId, PageRequest pageRequest);
}
