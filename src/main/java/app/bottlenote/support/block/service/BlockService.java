package app.bottlenote.support.block.service;

import app.bottlenote.support.block.domain.UserBlock;
import app.bottlenote.support.block.exception.BlockException;
import app.bottlenote.support.block.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static app.bottlenote.support.block.exception.BlockExceptionCode.CANNOT_BLOCK_SELF;
import static app.bottlenote.support.block.exception.BlockExceptionCode.REQUIRED_USER_ID;
import static app.bottlenote.support.block.exception.BlockExceptionCode.USER_ALREADY_BLOCKED;
import static app.bottlenote.support.block.exception.BlockExceptionCode.USER_BLOCK_NOT_FOUND;

/**
 * 사용자 차단 관리 서비스
 * 차단 관계 생성, 해제, 조회 및 캐싱 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {

	private final UserBlockRepository userBlockRepository;

	/**
	 * 사용자 차단
	 *
	 * @param blockerId 차단하는 사용자 ID
	 * @param blockedId 차단당하는 사용자 ID
	 * @param reason    차단 사유 (선택사항)
	 */
	@Transactional
	@CacheEvict(value = "blocked_users", key = "#blockerId")
	public void blockUser(Long blockerId, Long blockedId, String reason) {
		validateBlockRequest(blockerId, blockedId);

		// 이미 차단된 관계인지 확인
		if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
			throw new BlockException(USER_ALREADY_BLOCKED);
		}

		// 차단 관계 생성
		UserBlock userBlock = UserBlock.create(blockerId, blockedId, reason);
		userBlockRepository.save(userBlock);

		log.info("사용자 차단 완료 - 차단자: {}, 피차단자: {}", blockerId, blockedId);
	}

	/**
	 * 사용자 차단 (사유 없음)
	 */
	@Transactional
	public void blockUser(Long blockerId, Long blockedId) {
		blockUser(blockerId, blockedId, null);
	}

	/**
	 * 사용자 차단 해제
	 *
	 * @param blockerId 차단 해제하는 사용자 ID
	 * @param blockedId 차단 해제당하는 사용자 ID
	 */
	@Transactional
	@CacheEvict(value = "blocked_users", key = "#blockerId")
	public void unblockUser(Long blockerId, Long blockedId) {
		validateBlockRequest(blockerId, blockedId);

		UserBlock userBlock = userBlockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
				.orElseThrow(() -> new BlockException(USER_BLOCK_NOT_FOUND));

		userBlockRepository.delete(userBlock);

		log.info("사용자 차단 해제 완료 - 차단자: {}, 피차단자: {}", blockerId, blockedId);
	}

	/**
	 * 특정 사용자가 차단한 사용자들의 ID 목록을 조회 (캐시 적용)
	 *
	 * @param userId 사용자 ID
	 * @return 차단된 사용자 ID 목록
	 */
	@Transactional(readOnly = true)
	@Cacheable(value = "blocked_users", key = "#userId")
	public Set<Long> getBlockedUserIds(Long userId) {
		if (userId == null) {
			return Set.of();
		}

		return userBlockRepository.findBlockedUserIdsByBlockerId(userId);
	}

	/**
	 * 두 사용자 간 차단 여부 확인
	 *
	 * @param blockerId 차단하는 사용자 ID
	 * @param blockedId 차단당하는 사용자 ID
	 * @return 차단 여부
	 */
	@Transactional(readOnly = true)
	public boolean isBlocked(Long blockerId, Long blockedId) {
		if (blockerId == null || blockedId == null) {
			return false;
		}

		Set<Long> blockedUsers = getBlockedUserIds(blockerId);
		return blockedUsers.contains(blockedId);
	}

	/**
	 * 상호 차단 여부 확인
	 *
	 * @param userId1 사용자 1 ID
	 * @param userId2 사용자 2 ID
	 * @return 상호 차단 여부
	 */
	@Transactional(readOnly = true)
	public boolean isMutualBlocked(Long userId1, Long userId2) {
		if (userId1 == null || userId2 == null) {
			return false;
		}

		return userBlockRepository.existsMutualBlock(userId1, userId2);
	}

	/**
	 * 특정 사용자를 차단한 사용자 수 조회
	 *
	 * @param userId 사용자 ID
	 * @return 해당 사용자를 차단한 사용자 수
	 */
	@Transactional(readOnly = true)
	public long getBlockedByCount(Long userId) {
		if (userId == null) {
			return 0L;
		}

		return userBlockRepository.countByBlockedId(userId);
	}

	/**
	 * 특정 사용자가 차단한 사용자 수 조회
	 *
	 * @param userId 사용자 ID
	 * @return 해당 사용자가 차단한 사용자 수
	 */
	@Transactional(readOnly = true)
	public long getBlockingCount(Long userId) {
		if (userId == null) {
			return 0L;
		}

		return userBlockRepository.countByBlockerId(userId);
	}

	/**
	 * 차단 요청 유효성 검증
	 */
	private void validateBlockRequest(Long blockerId, Long blockedId) {
		if (blockerId == null) {
			throw new BlockException(REQUIRED_USER_ID);
		}

		if (blockedId == null) {
			throw new BlockException(REQUIRED_USER_ID);
		}

		if (blockerId.equals(blockedId)) {
			throw new BlockException(CANNOT_BLOCK_SELF);
		}
	}

}
