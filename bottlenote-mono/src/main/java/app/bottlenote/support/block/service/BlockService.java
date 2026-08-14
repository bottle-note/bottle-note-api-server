package app.bottlenote.support.block.service;

import static app.bottlenote.support.block.exception.BlockExceptionCode.CANNOT_BLOCK_SELF;
import static app.bottlenote.support.block.exception.BlockExceptionCode.REQUIRED_USER_ID;
import static app.bottlenote.support.block.exception.BlockExceptionCode.USER_ALREADY_BLOCKED;
import static app.bottlenote.support.block.exception.BlockExceptionCode.USER_BLOCK_NOT_FOUND;

import app.bottlenote.global.pagination.HmacCursorCodec;
import app.bottlenote.global.pagination.PageResponse;
import app.bottlenote.global.pagination.Pagination;
import app.bottlenote.global.pagination.TimeIdCursor;
import app.bottlenote.support.block.domain.UserBlock;
import app.bottlenote.support.block.domain.UserBlockRepository;
import app.bottlenote.support.block.dto.request.BlockPageableRequest;
import app.bottlenote.support.block.dto.response.UserBlockItem;
import app.bottlenote.support.block.dto.response.UserBlockListResponse;
import app.bottlenote.support.block.exception.BlockException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {

  private final UserBlockRepository userBlockRepository;
  private final HmacCursorCodec cursorCodec;

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

    UserBlock userBlock =
        userBlockRepository
            .findByBlockerIdAndBlockedId(blockerId, blockedId)
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
  public PageResponse<UserBlockListResponse> getBlockedUserItems(
      Long userId, BlockPageableRequest request) {
    if (userId == null) {
      return PageResponse.of(new UserBlockListResponse(List.of()), new Pagination(false, null));
    }
    List<UserBlockItem> items =
        userBlockRepository.findBlockedUserItemsByBlockerId(userId).stream()
            .sorted(
                Comparator.comparing(UserBlockItem::blockedAt, Comparator.reverseOrder())
                    .thenComparing(UserBlockItem::userId, Comparator.reverseOrder()))
            .toList();
    String context = "block.list:" + userId;
    if (request.cursor() != null) {
      var claims = cursorCodec.verify(request.cursor(), context);
      LocalDateTime lastAt = TimeIdCursor.time(claims);
      Long lastId = TimeIdCursor.id(claims);
      items =
          items.stream()
              .filter(
                  item ->
                      item.blockedAt().isBefore(lastAt)
                          || (item.blockedAt().equals(lastAt) && item.userId() < lastId))
              .toList();
    }
    Pagination.PageSlice<UserBlockItem> slice =
        Pagination.fromOverflow(
            items,
            request.size(),
            item ->
                cursorCodec.encode(context, TimeIdCursor.keys(item.blockedAt(), item.userId())));
    return PageResponse.of(new UserBlockListResponse(slice.items()), slice.pagination());
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
