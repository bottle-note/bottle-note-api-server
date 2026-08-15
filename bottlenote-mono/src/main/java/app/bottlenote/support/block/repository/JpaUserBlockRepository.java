package app.bottlenote.support.block.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.support.block.domain.UserBlock;
import app.bottlenote.support.block.domain.UserBlockRepository;
import app.bottlenote.support.block.dto.response.UserBlockItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@JpaRepositoryImpl
public interface JpaUserBlockRepository
    extends UserBlockRepository, JpaRepository<UserBlock, Long> {

  @Override
  @Query(
      """
		SELECT ub.blockedId FROM userBlock ub WHERE ub.blockerId = :blockerId
		""")
  Set<Long> findBlockedUserIdsByBlockerId(@Param("blockerId") Long blockerId);

  @Override
  boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

  @Override
  Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

  @Override
  long countByBlockedId(Long blockedId);

  @Override
  long countByBlockerId(Long blockerId);

  @Override
  @Query(
      """
		SELECT COUNT(ub) = 2 FROM userBlock ub
		WHERE (ub.blockerId = :userId1 AND ub.blockedId = :userId2)
		OR (ub.blockerId = :userId2 AND ub.blockedId = :userId1)
		""")
  boolean existsMutualBlock(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

  @Override
  default List<UserBlockItem> findBlockedUserItemsByBlockerId(
      Long blockerId, LocalDateTime lastBlockedAt, Long lastUserId, int limit) {
    return searchBlockedUserItems(blockerId, lastBlockedAt, lastUserId, PageRequest.of(0, limit));
  }

  @Query(
      """
		SELECT new app.bottlenote.support.block.dto.response.UserBlockItem(u.id, u.nickName, ub.createAt)
		FROM userBlock ub JOIN users u ON ub.blockedId = u.id
		WHERE ub.blockerId = :blockerId
		AND (:lastBlockedAt IS NULL
			OR ub.createAt < :lastBlockedAt
			OR (ub.createAt = :lastBlockedAt AND u.id < :lastUserId))
		ORDER BY ub.createAt DESC, u.id DESC
		""")
  List<UserBlockItem> searchBlockedUserItems(
      @Param("blockerId") Long blockerId,
      @Param("lastBlockedAt") LocalDateTime lastBlockedAt,
      @Param("lastUserId") Long lastUserId,
      Pageable pageable);
}
