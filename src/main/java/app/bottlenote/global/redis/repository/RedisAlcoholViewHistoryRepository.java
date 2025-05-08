package app.bottlenote.global.redis.repository;

import app.bottlenote.global.redis.entity.AlcoholViewHistory;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface RedisAlcoholViewHistoryRepository extends CrudRepository<AlcoholViewHistory, UUID> {

	// 특정 사용자의 모든 조회 기록 가져오기 (최신순)
	List<AlcoholViewHistory> findByUserIdOrderByViewTimeDesc(Long userId);

	// 특정 사용자가 특정 주류를 조회했는지 확인
	boolean existsByUserIdAndAlcoholId(Long userId, Long alcoholId);

	// 특정 사용자의 특정 주류 조회 기록 삭제
	void deleteByUserIdAndAlcoholId(Long userId, Long alcoholId);

	// 특정 사용자의 모든 조회 기록 삭제
	void deleteByUserId(Long userId);

}
