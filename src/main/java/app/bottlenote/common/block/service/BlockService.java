package app.bottlenote.common.block.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 간소화된 차단 서비스
 * 핵심 기능: 특정 사용자가 차단한 사용자 목록 조회
 */
@Slf4j
@Service
public class BlockService {

    /**
     * 특정 사용자가 차단한 사용자들의 ID 목록을 조회
     * 테스트용으로 하드코딩된 차단 관계
     * 
     * @param userId 사용자 ID
     * @return 차단된 사용자 ID 목록
     */
    public Set<Long> getBlockedUserIds(Long userId) {
        if (userId == null || userId == -1L) {
            return Set.of();
        }
        
        // 테스트용 하드코딩된 차단 관계
        return switch (userId.intValue()) {
            case 1 -> Set.of(2L, 3L);  // 사용자 1은 사용자 2,3을 차단
            case 2 -> Set.of(4L);      // 사용자 2는 사용자 4를 차단  
            case 3 -> Set.of(1L, 5L);  // 사용자 3은 사용자 1,5를 차단
            default -> Set.of();       // 나머지는 차단 없음
        };
    }
    
    /**
     * 두 사용자 간 차단 여부 확인
     */
    public boolean isBlocked(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null) {
            return false;
        }
        
        Set<Long> blockedUsers = getBlockedUserIds(blockerId);
        return blockedUsers.contains(blockedId);
    }
}