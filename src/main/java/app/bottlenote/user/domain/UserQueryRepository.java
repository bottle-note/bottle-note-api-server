package app.bottlenote.user.domain;

import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 유저 정보에 관한 질의에 대한 애그리거트를 정의합니다.
 */
public interface UserQueryRepository {

	User save(User User);

	Optional<User> findById(Long UserId);

	List<User> findAll();

	List<User> findAllByIdIn(List<Long> ids);

	Boolean existsByUserId(Long userId);

	Long countByUsername(String userName);

	MyPageResponse getMyPage(Long userId, Long currentUserId);

	MyBottleResponse getMyBottle(MyBottlePageableCriteria criteria);

	@Query(value = "SELECT u.* FROM users u " +
		"LEFT JOIN picks p ON p.user_id = u.id AND p.status = 'PICK' " +
		"LEFT JOIN reviews r ON r.user_id = u.id AND r.active_status = 'ACTIVE' " +
		"LEFT JOIN ratings rt ON rt.user_id = u.id AND rt.rating_point > 0.0 " +
		"GROUP BY u.id " +
		"ORDER BY COUNT(DISTINCT p.id) + COUNT(DISTINCT r.id) + COUNT(DISTINCT rt.id) DESC " +
		"LIMIT 1",
		nativeQuery = true)
	Optional<User> findMostActiveUserNative();

}
