package app.bottlenote.support.block.repository;

import app.bottlenote.support.block.domain.UserBlock;
import app.bottlenote.support.block.dto.response.UserBlockItem;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
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
  @Query(
      """
		SELECT new app.bottlenote.support.block.dto.response.UserBlockItem(u.id, u.nickName, ub.createAt)
		FROM userBlock ub JOIN users u ON ub.blockedId = u.id
		WHERE ub.blockerId = :blockerId
		""")
  List<UserBlockItem> findBlockedUserItemsByBlockerId(@Param("blockerId") Long blockerId);
}
