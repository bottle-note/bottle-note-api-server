package app.bottlenote.user.domain;

import app.bottlenote.user.dto.dsl.MyBottlePageableCriteria;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;

import java.util.List;
import java.util.Optional;

/**
 * 유저 정보에 관한 질의에 대한 애그리거트를 정의합니다.
 */
public interface UserRepository {

	User save(User User);

	Optional<User> findById(Long UserId);

	List<User> findAll();

	List<User> findAllByIdIn(List<Long> ids);

	Boolean existsByUserId(Long userId);

	Long countByUsername(String userName);

	MyPageResponse getMyPage(Long userId, Long currentUserId);

	MyBottleResponse getMyBottle(MyBottlePageableCriteria criteria);

	boolean existsByNickName(String nickname);
}
