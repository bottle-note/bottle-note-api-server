package app.bottlenote.support.block.repository;

import app.bottlenote.support.block.domain.UserBlock;

import java.util.Optional;
import java.util.Set;

/**
 * 사용자 차단 관계 Repository
 */
public interface UserBlockRepository {

    /**
     * 차단 관계 저장
     */
    UserBlock save(UserBlock userBlock);

    /**
     * 차단 관계 삭제
     */
    void delete(UserBlock userBlock);

    /**
     * 특정 사용자가 차단한 사용자 ID 목록 조회
     * 
     * @param blockerId 차단한 사용자 ID
     * @return 차단된 사용자 ID 목록
     */
    Set<Long> findBlockedUserIdsByBlockerId(Long blockerId);

    /**
     * 차단 관계 존재 여부 확인
     * 
     * @param blockerId 차단한 사용자 ID
     * @param blockedId 차단당한 사용자 ID
     * @return 차단 관계 존재 여부
     */
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /**
     * 특정 차단 관계 조회
     * 
     * @param blockerId 차단한 사용자 ID
     * @param blockedId 차단당한 사용자 ID
     * @return 차단 관계 엔티티
     */
    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /**
     * 특정 사용자를 차단한 사용자 수 조회
     * 
     * @param blockedId 차단당한 사용자 ID
     * @return 차단한 사용자 수
     */
    long countByBlockedId(Long blockedId);

    /**
     * 특정 사용자가 차단한 사용자 수 조회
     * 
     * @param blockerId 차단한 사용자 ID
     * @return 차단한 사용자 수
     */
    long countByBlockerId(Long blockerId);

    /**
     * 상호 차단 관계 확인 (서로 차단한 경우)
     * 
     * @param userId1 사용자 1
     * @param userId2 사용자 2
     * @return 상호 차단 여부
     */
    boolean existsMutualBlock(Long userId1, Long userId2);
}