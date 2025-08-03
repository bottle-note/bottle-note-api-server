package app.bottlenote.support.block.service;

import app.bottlenote.global.data.response.CollectionResponse;
import app.bottlenote.support.block.domain.UserBlock;
import app.bottlenote.support.block.dto.response.UserBlockItem;
import app.bottlenote.support.block.exception.BlockException;
import app.bottlenote.support.block.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static app.bottlenote.support.block.exception.BlockExceptionCode.CANNOT_BLOCK_SELF;
import static app.bottlenote.support.block.exception.BlockExceptionCode.REQUIRED_USER_ID;
import static app.bottlenote.support.block.exception.BlockExceptionCode.USER_ALREADY_BLOCKED;
import static app.bottlenote.support.block.exception.BlockExceptionCode.USER_BLOCK_NOT_FOUND;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {

	private final UserBlockRepository userBlockRepository;

	@Transactional
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

	@Transactional
	public void blockUser(Long blockerId, Long blockedId) {
		blockUser(blockerId, blockedId, null);
	}

	@Transactional
	public void unblockUser(Long blockerId, Long blockedId) {
		validateBlockRequest(blockerId, blockedId);

		UserBlock userBlock = userBlockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
				.orElseThrow(() -> new BlockException(USER_BLOCK_NOT_FOUND));

		userBlockRepository.delete(userBlock);

		log.info("사용자 차단 해제 완료 - 차단자: {}, 피차단자: {}", blockerId, blockedId);
	}

	@Transactional(readOnly = true)
	public Set<Long> getBlockedUserIds(Long userId) {
		if (userId == null) {
			return Set.of();
		}

		return userBlockRepository.findBlockedUserIdsByBlockerId(userId);
	}

	@Transactional(readOnly = true)
	public CollectionResponse<UserBlockItem> getBlockedUserItems(Long userId) {
		if (userId == null) {
			return CollectionResponse.of(0L, List.of());
		}

		List<UserBlockItem> items = userBlockRepository.findBlockedUserItemsByBlockerId(userId);
		long totalCount = items.size();

		return CollectionResponse.of(totalCount, items);
	}

	@Transactional(readOnly = true)
	public boolean isBlocked(Long blockerId, Long blockedId) {
		if (blockerId == null || blockedId == null) {
			return false;
		}

		Set<Long> blockedUsers = getBlockedUserIds(blockerId);
		return blockedUsers.contains(blockedId);
	}

	@Transactional(readOnly = true)
	public boolean isMutualBlocked(Long userId1, Long userId2) {
		if (userId1 == null || userId2 == null) {
			return false;
		}

		return userBlockRepository.existsMutualBlock(userId1, userId2);
	}

	@Transactional(readOnly = true)
	public long getBlockedByCount(Long userId) {
		if (userId == null) {
			return 0L;
		}

		return userBlockRepository.countByBlockedId(userId);
	}

	@Transactional(readOnly = true)
	public long getBlockingCount(Long userId) {
		if (userId == null) {
			return 0L;
		}

		return userBlockRepository.countByBlockerId(userId);
	}

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
