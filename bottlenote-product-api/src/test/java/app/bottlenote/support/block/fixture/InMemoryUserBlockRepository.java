package app.bottlenote.support.block.fixture;

import app.bottlenote.support.block.domain.UserBlock;
import app.bottlenote.support.block.dto.response.UserBlockItem;
import app.bottlenote.support.block.domain.UserBlockRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.test.util.ReflectionTestUtils;

public class InMemoryUserBlockRepository implements UserBlockRepository {

  private final Map<Long, UserBlock> userBlocks = new HashMap<>();
  private final Map<String, String> userNames = new HashMap<>();
  private Long idGenerator = 1L;

  public InMemoryUserBlockRepository() {
    userNames.put("1", "테스트유저1");
    userNames.put("2", "테스트유저2");
    userNames.put("3", "테스트유저3");
    userNames.put("4", "테스트유저4");
    userNames.put("5", "테스트유저5");
  }

  @Override
  public UserBlock save(UserBlock userBlock) {
    Long id = idGenerator++;
    ReflectionTestUtils.setField(userBlock, "id", id);
    userBlocks.put(id, userBlock);
    return userBlock;
  }

  @Override
  public void delete(UserBlock userBlock) {
    userBlocks.remove(userBlock.getId());
  }

  @Override
  public Set<Long> findBlockedUserIdsByBlockerId(Long blockerId) {
    return userBlocks.values().stream()
        .filter(block -> block.getBlockerId().equals(blockerId))
        .map(UserBlock::getBlockedId)
        .collect(Collectors.toSet());
  }

  @Override
  public boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId) {
    return userBlocks.values().stream()
        .anyMatch(
            block ->
                block.getBlockerId().equals(blockerId) && block.getBlockedId().equals(blockedId));
  }

  @Override
  public Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId) {
    return userBlocks.values().stream()
        .filter(
            block ->
                block.getBlockerId().equals(blockerId) && block.getBlockedId().equals(blockedId))
        .findFirst();
  }

  @Override
  public long countByBlockedId(Long blockedId) {
    return userBlocks.values().stream()
        .filter(block -> block.getBlockedId().equals(blockedId))
        .count();
  }

  @Override
  public long countByBlockerId(Long blockerId) {
    return userBlocks.values().stream()
        .filter(block -> block.getBlockerId().equals(blockerId))
        .count();
  }

  @Override
  public boolean existsMutualBlock(Long userId1, Long userId2) {
    boolean user1BlocksUser2 = existsByBlockerIdAndBlockedId(userId1, userId2);
    boolean user2BlocksUser1 = existsByBlockerIdAndBlockedId(userId2, userId1);
    return user1BlocksUser2 && user2BlocksUser1;
  }

  @Override
  public List<UserBlockItem> findBlockedUserItemsByBlockerId(Long blockerId) {
    return userBlocks.values().stream()
        .filter(block -> block.getBlockerId().equals(blockerId))
        .map(
            block ->
                new UserBlockItem(
                    block.getBlockedId(),
                    userNames.getOrDefault(block.getBlockedId().toString(), "알 수 없는 사용자"),
                    LocalDateTime.now()))
        .collect(Collectors.toList());
  }

  public void addUser(Long userId, String userName) {
    userNames.put(userId.toString(), userName);
  }

  public void clear() {
    userBlocks.clear();
    idGenerator = 1L;
  }
}
