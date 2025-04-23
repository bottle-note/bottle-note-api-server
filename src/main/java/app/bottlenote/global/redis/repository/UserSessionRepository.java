package app.bottlenote.global.redis.repository;

import app.bottlenote.global.redis.entity.UserSession;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserSessionRepository extends CrudRepository<UserSession, String> {

	// 사용자 ID로 모든 세션 찾기
	List<UserSession> findByUserId(Long userId);

	// 사용자 ID와 디바이스 정보로 세션 찾기
	Optional<UserSession> findByUserIdAndDeviceInfo(Long userId, String deviceInfo);

	// 사용자 ID로 세션 삭제
	void deleteByUserId(Long userId);
}
