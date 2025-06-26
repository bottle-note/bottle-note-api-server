package app.bottlenote.global.service;

import app.bottlenote.user.domain.UserBlock;
import app.bottlenote.user.domain.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 사용자 차단 기능을 제공하는 서비스
 * DB 기반으로 차단 관계를 관리하며, Redis 캐시를 통해 성능을 최적화합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {

    private final UserBlockRepository userBlockRepository;

    /**
     * 특정 사용자가 차단한 사용자들의 ID 목록을 조회
     * Redis 캐시를 통해 성능 최적화
     *
     * @param userId 차단한 사용자 ID
     * @return 차단된 사용자 ID 목록
     */
    @Cacheable(
        value = "blocked_users", 
        key = "#userId", 
        unless = "#userId == null or #userId == -1",
        condition = "#userId != null and #userId != -1"
    )
    @Transactional(readOnly = true)
    public Set<Long> getBlockedUserIds(Long userId) {
        if (userId == null || userId == -1L) {
            return Collections.emptySet();
        }

        try {
            Set<Long> blockedUsers = userBlockRepository.findBlockedUserIdsByBlockerId(userId);
            log.debug("사용자 {}의 차단 목록 조회: {} 명", userId, blockedUsers.size());
            return blockedUsers;
        } catch (Exception e) {
            log.error("차단 목록 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            return Collections.emptySet();
        }
    }

    /**
     * 특정 사용자가 다른 사용자를 차단했는지 확인
     *
     * @param blockerId 차단한 사용자 ID
     * @param blockedId 차단 대상 사용자 ID
     * @return 차단 여부
     */
    @Transactional(readOnly = true)
    public boolean isBlocked(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null || blockerId == -1L || blockerId.equals(blockedId)) {
            return false;
        }

        try {
            return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
        } catch (Exception e) {
            log.error("차단 여부 확인 실패 - blockerId: {}, blockedId: {}, error: {}", 
                     blockerId, blockedId, e.getMessage());
            return false;
        }
    }

    /**
     * 상호 차단 관계 확인 (서로 차단한 경우)
     *
     * @param userId1 사용자 1
     * @param userId2 사용자 2
     * @return 상호 차단 여부
     */
    @Transactional(readOnly = true)
    public boolean isMutuallyBlocked(Long userId1, Long userId2) {
        if (userId1 == null || userId2 == null || userId1.equals(userId2)) {
            return false;
        }

        try {
            return userBlockRepository.existsMutualBlock(userId1, userId2);
        } catch (Exception e) {
            log.error("상호 차단 확인 실패 - userId1: {}, userId2: {}, error: {}", 
                     userId1, userId2, e.getMessage());
            return false;
        }
    }

    /**
     * 배치로 여러 사용자의 차단 목록 조회 (성능 최적화)
     *
     * @param userIds 조회할 사용자 ID 목록
     * @return 사용자별 차단 목록 맵
     */
    @Transactional(readOnly = true)
    public Map<Long, Set<Long>> getBulkBlockedUsers(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return userIds.stream()
            .filter(id -> id != null && id != -1L)
            .collect(Collectors.toMap(
                Function.identity(),
                this::getBlockedUserIds,
                (existing, replacement) -> existing // 중복 키 처리
            ));
    }

    /**
     * 사용자 차단 (실제 운영용 메서드)
     *
     * @param currentUserId 차단하는 사용자 ID
     * @param targetUserId 차단할 사용자 ID
     * @param reason 차단 사유
     * @return 생성된 차단 관계
     */
    @Transactional
    @CacheEvict(value = "blocked_users", key = "#currentUserId")
    public UserBlock blockUser(Long currentUserId, Long targetUserId, String reason) {
        validateBlockRequest(currentUserId, targetUserId);
        return createBlock(currentUserId, targetUserId, reason);
    }

    /**
     * 사용자 차단 해제 (실제 운영용 메서드)
     *
     * @param currentUserId 차단 해제하는 사용자 ID
     * @param targetUserId 차단 해제할 사용자 ID
     */
    @Transactional
    @CacheEvict(value = "blocked_users", key = "#currentUserId")
    public void unblockUser(Long currentUserId, Long targetUserId) {
        validateBlockRequest(currentUserId, targetUserId);
        removeBlock(currentUserId, targetUserId);
    }

    /**
     * 차단 관계 생성 (내부용 - 테스트에서도 사용)
     *
     * @param blockerId 차단한 사용자 ID
     * @param blockedId 차단될 사용자 ID
     * @param reason 차단 사유
     * @return 생성된 차단 관계
     */
    @Transactional
    @CacheEvict(value = "blocked_users", key = "#blockerId")
    public UserBlock createBlock(Long blockerId, Long blockedId, String reason) {
        validateBlockRequest(blockerId, blockedId);

        // 이미 차단된 관계인지 확인
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            log.warn("이미 차단된 관계입니다 - blockerId: {}, blockedId: {}", blockerId, blockedId);
            return userBlockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElseThrow(() -> new IllegalStateException("차단 관계 조회 실패"));
        }

        try {
            UserBlock userBlock = UserBlock.create(blockerId, blockedId, reason);
            UserBlock savedBlock = userBlockRepository.save(userBlock);
            
            log.info("차단 관계 생성 완료 - blockerId: {}, blockedId: {}, reason: {}", 
                    blockerId, blockedId, reason);
            
            return savedBlock;
        } catch (Exception e) {
            log.error("차단 관계 생성 실패 - blockerId: {}, blockedId: {}, error: {}", 
                     blockerId, blockedId, e.getMessage());
            throw new RuntimeException("차단 관계 생성에 실패했습니다.", e);
        }
    }

    /**
     * 차단 관계 해제
     *
     * @param blockerId 차단한 사용자 ID
     * @param blockedId 차단 해제할 사용자 ID
     */
    @Transactional
    @CacheEvict(value = "blocked_users", key = "#blockerId")
    public void removeBlock(Long blockerId, Long blockedId) {
        validateBlockRequest(blockerId, blockedId);

        try {
            UserBlock userBlock = userBlockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .orElse(null);

            if (userBlock != null) {
                userBlockRepository.delete(userBlock);
                log.info("차단 관계 해제 완료 - blockerId: {}, blockedId: {}", blockerId, blockedId);
            } else {
                log.warn("해제할 차단 관계가 없습니다 - blockerId: {}, blockedId: {}", blockerId, blockedId);
            }
        } catch (Exception e) {
            log.error("차단 관계 해제 실패 - blockerId: {}, blockedId: {}, error: {}", 
                     blockerId, blockedId, e.getMessage());
            throw new RuntimeException("차단 관계 해제에 실패했습니다.", e);
        }
    }

    /**
     * 차단 요청 유효성 검증
     */
    private void validateBlockRequest(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null) {
            throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다.");
        }
        
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("자기 자신을 차단할 수 없습니다.");
        }
        
        if (blockerId == -1L) {
            throw new IllegalArgumentException("익명 사용자는 차단 기능을 사용할 수 없습니다.");
        }
    }

    /**
     * 특정 사용자의 차단 통계 조회
     *
     * @param userId 사용자 ID
     * @return 차단 통계 정보
     */
    @Transactional(readOnly = true)
    public BlockStatistics getBlockStatistics(Long userId) {
        if (userId == null || userId == -1L) {
            return new BlockStatistics(0L, 0L);
        }

        try {
            long blockedCount = userBlockRepository.countByBlockerId(userId);
            long blockedByCount = userBlockRepository.countByBlockedId(userId);
            
            return new BlockStatistics(blockedCount, blockedByCount);
        } catch (Exception e) {
            log.error("차단 통계 조회 실패 - userId: {}, error: {}", userId, e.getMessage());
            return new BlockStatistics(0L, 0L);
        }
    }

    /**
     * 차단 통계 정보를 담는 record
     */
    public record BlockStatistics(
        long blockedCount,      // 내가 차단한 사용자 수
        long blockedByCount     // 나를 차단한 사용자 수
    ) {}
}
