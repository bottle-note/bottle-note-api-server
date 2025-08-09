package app.bottlenote.support.block.service;

import static app.bottlenote.support.block.exception.BlockExceptionCode.CANNOT_BLOCK_SELF;
import static app.bottlenote.support.block.exception.BlockExceptionCode.REQUIRED_USER_ID;
import static app.bottlenote.support.block.exception.BlockExceptionCode.USER_ALREADY_BLOCKED;
import static app.bottlenote.support.block.exception.BlockExceptionCode.USER_BLOCK_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bottlenote.global.data.response.CollectionResponse;
import app.bottlenote.support.block.dto.response.UserBlockItem;
import app.bottlenote.support.block.exception.BlockException;
import app.bottlenote.support.block.fixture.InMemoryUserBlockRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("unit")
@DisplayName("[unit] [service] BlockService")
class BlockServiceTest {

  private BlockService blockService;
  private InMemoryUserBlockRepository userBlockRepository;

  @BeforeEach
  void setUp() {
    userBlockRepository = new InMemoryUserBlockRepository();
    blockService = new BlockService(userBlockRepository);

    userBlockRepository.addUser(1L, "차단자");
    userBlockRepository.addUser(2L, "피차단자1");
    userBlockRepository.addUser(3L, "피차단자2");
    userBlockRepository.addUser(4L, "피차단자3");
  }

  @Nested
  @DisplayName("사용자를 차단할 때")
  class BlockUser {

    @Test
    @DisplayName("정상적으로 차단할 수 있다")
    void 정상적으로_차단할_수_있다() {
      // given
      Long blockerId = 1L;
      Long blockedId = 2L;
      String reason = "스팸 메시지";

      // when
      blockService.blockUser(blockerId, blockedId, reason);

      // then
      assertTrue(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId));
    }

    @Test
    @DisplayName("사유 없이 차단할 수 있다")
    void 사유_없이_차단할_수_있다() {
      // given
      Long blockerId = 1L;
      Long blockedId = 2L;

      // when
      blockService.blockUser(blockerId, blockedId);

      // then
      assertTrue(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId));
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("차단자 ID가 null이면 예외가 발생한다")
    void 차단자_ID가_null이면_예외가_발생한다(Long blockerId) {
      // given
      Long blockedId = 2L;

      // when & then
      BlockException exception =
          assertThrows(BlockException.class, () -> blockService.blockUser(blockerId, blockedId));
      assertEquals(REQUIRED_USER_ID, exception.getExceptionCode());
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("피차단자 ID가 null이면 예외가 발생한다")
    void 피차단자_ID가_null이면_예외가_발생한다(Long blockedId) {
      // given
      Long blockerId = 1L;

      // when & then
      BlockException exception =
          assertThrows(BlockException.class, () -> blockService.blockUser(blockerId, blockedId));
      assertEquals(REQUIRED_USER_ID, exception.getExceptionCode());
    }

    @Test
    @DisplayName("자기 자신을 차단하려고 하면 예외가 발생한다")
    void 자기_자신을_차단하려고_하면_예외가_발생한다() {
      // given
      Long userId = 1L;

      // when & then
      BlockException exception =
          assertThrows(BlockException.class, () -> blockService.blockUser(userId, userId));
      assertEquals(CANNOT_BLOCK_SELF, exception.getExceptionCode());
    }

    @Test
    @DisplayName("이미 차단된 사용자를 다시 차단하려고 하면 예외가 발생한다")
    void 이미_차단된_사용자를_다시_차단하려고_하면_예외가_발생한다() {
      // given
      Long blockerId = 1L;
      Long blockedId = 2L;
      blockService.blockUser(blockerId, blockedId);

      // when & then
      BlockException exception =
          assertThrows(BlockException.class, () -> blockService.blockUser(blockerId, blockedId));
      assertEquals(USER_ALREADY_BLOCKED, exception.getExceptionCode());
    }
  }

  @Nested
  @DisplayName("사용자 차단을 해제할 때")
  class UnblockUser {

    @Test
    @DisplayName("정상적으로 차단을 해제할 수 있다")
    void 정상적으로_차단을_해제할_수_있다() {
      // given
      Long blockerId = 1L;
      Long blockedId = 2L;
      blockService.blockUser(blockerId, blockedId);

      // when
      blockService.unblockUser(blockerId, blockedId);

      // then
      assertFalse(userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId));
    }

    @Test
    @DisplayName("차단하지 않은 사용자를 해제하려고 하면 예외가 발생한다")
    void 차단하지_않은_사용자를_해제하려고_하면_예외가_발생한다() {
      // given
      Long blockerId = 1L;
      Long blockedId = 2L;

      // when & then
      BlockException exception =
          assertThrows(BlockException.class, () -> blockService.unblockUser(blockerId, blockedId));
      assertEquals(USER_BLOCK_NOT_FOUND, exception.getExceptionCode());
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("차단자 ID가 null이면 예외가 발생한다")
    void 차단자_ID가_null이면_예외가_발생한다(Long blockerId) {
      // given
      Long blockedId = 2L;

      // when & then
      BlockException exception =
          assertThrows(BlockException.class, () -> blockService.unblockUser(blockerId, blockedId));
      assertEquals(REQUIRED_USER_ID, exception.getExceptionCode());
    }
  }

  @Nested
  @DisplayName("차단된 사용자 ID 목록을 조회할 때")
  class GetBlockedUserIds {

    @Test
    @DisplayName("차단한 사용자들의 ID를 조회할 수 있다")
    void 차단한_사용자들의_ID를_조회할_수_있다() {
      // given
      Long blockerId = 1L;
      blockService.blockUser(blockerId, 2L);
      blockService.blockUser(blockerId, 3L);

      // when
      Set<Long> blockedUserIds = blockService.getBlockedUserIds(blockerId);

      // then
      assertEquals(2, blockedUserIds.size());
      assertTrue(blockedUserIds.contains(2L));
      assertTrue(blockedUserIds.contains(3L));
    }

    @Test
    @DisplayName("차단한 사용자가 없으면 빈 Set을 반환한다")
    void 차단한_사용자가_없으면_빈_Set을_반환한다() {
      // given
      Long blockerId = 1L;

      // when
      Set<Long> blockedUserIds = blockService.getBlockedUserIds(blockerId);

      // then
      assertTrue(blockedUserIds.isEmpty());
    }

    @Test
    @DisplayName("사용자 ID가 null이면 빈 Set을 반환한다")
    void 사용자_ID가_null이면_빈_Set을_반환한다() {
      // when
      Set<Long> blockedUserIds = blockService.getBlockedUserIds(null);

      // then
      assertTrue(blockedUserIds.isEmpty());
    }
  }

  @Nested
  @DisplayName("차단된 사용자 상세 정보를 조회할 때")
  class GetBlockedUserItems {

    @Test
    @DisplayName("차단한 사용자들의 상세 정보를 조회할 수 있다")
    void 차단한_사용자들의_상세_정보를_조회할_수_있다() {
      // given
      Long blockerId = 1L;
      blockService.blockUser(blockerId, 2L);
      blockService.blockUser(blockerId, 3L);

      // when
      CollectionResponse<UserBlockItem> response = blockService.getBlockedUserItems(blockerId);

      // then
      assertEquals(2L, response.getTotalCount());
      assertEquals(2, response.getItems().size());

      UserBlockItem item1 =
          response.getItems().stream()
              .filter(item -> item.userId().equals(2L))
              .findFirst()
              .orElseThrow();
      assertEquals("피차단자1", item1.userName());
      assertNotNull(item1.blockedAt());
    }

    @Test
    @DisplayName("차단한 사용자가 없으면 빈 목록을 반환한다")
    void 차단한_사용자가_없으면_빈_목록을_반환한다() {
      // given
      Long blockerId = 1L;

      // when
      CollectionResponse<UserBlockItem> response = blockService.getBlockedUserItems(blockerId);

      // then
      assertEquals(0L, response.getTotalCount());
      assertTrue(response.getItems().isEmpty());
    }

    @Test
    @DisplayName("사용자 ID가 null이면 빈 목록을 반환한다")
    void 사용자_ID가_null이면_빈_목록을_반환한다() {
      // when
      CollectionResponse<UserBlockItem> response = blockService.getBlockedUserItems(null);

      // then
      assertEquals(0L, response.getTotalCount());
      assertTrue(response.getItems().isEmpty());
    }
  }

  @Nested
  @DisplayName("차단 여부를 확인할 때")
  class IsBlocked {

    @Test
    @DisplayName("차단된 사용자는 true를 반환한다")
    void 차단된_사용자는_true를_반환한다() {
      // given
      Long blockerId = 1L;
      Long blockedId = 2L;
      blockService.blockUser(blockerId, blockedId);

      // when
      boolean isBlocked = blockService.isBlocked(blockerId, blockedId);

      // then
      assertTrue(isBlocked);
    }

    @Test
    @DisplayName("차단되지 않은 사용자는 false를 반환한다")
    void 차단되지_않은_사용자는_false를_반환한다() {
      // given
      Long blockerId = 1L;
      Long blockedId = 2L;

      // when
      boolean isBlocked = blockService.isBlocked(blockerId, blockedId);

      // then
      assertFalse(isBlocked);
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("차단자 ID가 null이면 false를 반환한다")
    void 차단자_ID가_null이면_false를_반환한다(Long blockerId) {
      // given
      Long blockedId = 2L;

      // when
      boolean isBlocked = blockService.isBlocked(blockerId, blockedId);

      // then
      assertFalse(isBlocked);
    }
  }

  @Nested
  @DisplayName("상호 차단 여부를 확인할 때")
  class IsMutualBlocked {

    @Test
    @DisplayName("서로 차단한 경우 true를 반환한다")
    void 서로_차단한_경우_true를_반환한다() {
      // given
      Long userId1 = 1L;
      Long userId2 = 2L;
      blockService.blockUser(userId1, userId2);
      blockService.blockUser(userId2, userId1);

      // when
      boolean isMutualBlocked = blockService.isMutualBlocked(userId1, userId2);

      // then
      assertTrue(isMutualBlocked);
    }

    @Test
    @DisplayName("한쪽만 차단한 경우 false를 반환한다")
    void 한쪽만_차단한_경우_false를_반환한다() {
      // given
      Long userId1 = 1L;
      Long userId2 = 2L;
      blockService.blockUser(userId1, userId2);

      // when
      boolean isMutualBlocked = blockService.isMutualBlocked(userId1, userId2);

      // then
      assertFalse(isMutualBlocked);
    }

    @Test
    @DisplayName("둘 다 차단하지 않은 경우 false를 반환한다")
    void 둘_다_차단하지_않은_경우_false를_반환한다() {
      // given
      Long userId1 = 1L;
      Long userId2 = 2L;

      // when
      boolean isMutualBlocked = blockService.isMutualBlocked(userId1, userId2);

      // then
      assertFalse(isMutualBlocked);
    }
  }

  @Nested
  @DisplayName("차단 통계를 조회할 때")
  class BlockStatistics {

    @Test
    @DisplayName("특정 사용자를 차단한 사용자 수를 조회할 수 있다")
    void 특정_사용자를_차단한_사용자_수를_조회할_수_있다() {
      // given
      Long targetUserId = 2L;
      blockService.blockUser(1L, targetUserId);
      blockService.blockUser(3L, targetUserId);
      blockService.blockUser(4L, targetUserId);

      // when
      long blockedByCount = blockService.getBlockedByCount(targetUserId);

      // then
      assertEquals(3L, blockedByCount);
    }

    @Test
    @DisplayName("특정 사용자가 차단한 사용자 수를 조회할 수 있다")
    void 특정_사용자가_차단한_사용자_수를_조회할_수_있다() {
      // given
      Long blockerId = 1L;
      blockService.blockUser(blockerId, 2L);
      blockService.blockUser(blockerId, 3L);
      blockService.blockUser(blockerId, 4L);

      // when
      long blockingCount = blockService.getBlockingCount(blockerId);

      // then
      assertEquals(3L, blockingCount);
    }

    @ParameterizedTest
    @ValueSource(longs = {999L})
    @NullSource
    @DisplayName("존재하지 않는 사용자의 통계는 0을 반환한다")
    void 존재하지_않는_사용자의_통계는_0을_반환한다(Long userId) {
      // when
      long blockedByCount = blockService.getBlockedByCount(userId);
      long blockingCount = blockService.getBlockingCount(userId);

      // then
      assertEquals(0L, blockedByCount);
      assertEquals(0L, blockingCount);
    }
  }
}
