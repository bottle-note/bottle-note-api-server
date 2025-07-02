package app.bottlenote.support.block.repository;

import app.bottlenote.support.block.domain.UserBlock;
import app.bottlenote.support.block.dto.response.UserBlockItem;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserBlockRepository {

    UserBlock save(UserBlock userBlock);

    void delete(UserBlock userBlock);

    Set<Long> findBlockedUserIdsByBlockerId(Long blockerId);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    long countByBlockedId(Long blockedId);

    long countByBlockerId(Long blockerId);

    boolean existsMutualBlock(Long userId1, Long userId2);

    List<UserBlockItem> findBlockedUserItemsByBlockerId(Long blockerId);
}