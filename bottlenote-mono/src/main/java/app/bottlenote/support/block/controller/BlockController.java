package app.bottlenote.support.block.controller;

import static app.bottlenote.support.block.exception.BlockExceptionCode.REQUIRED_USER_ID;

import app.bottlenote.global.data.response.CollectionResponse;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.support.block.dto.request.BlockCreateRequest;
import app.bottlenote.support.block.dto.response.UserBlockItem;
import app.bottlenote.support.block.exception.BlockException;
import app.bottlenote.support.block.service.BlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/blocks")
public class BlockController {

  private final BlockService blockService;

  @PostMapping
  public ResponseEntity<?> createBlock(@RequestBody @Valid BlockCreateRequest request) {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

    blockService.blockUser(currentUserId, request.blockedUserId());
    CollectionResponse<UserBlockItem> blockedUsers =
        blockService.getBlockedUserItems(currentUserId);

    return GlobalResponse.ok(blockedUsers);
  }

  @DeleteMapping("/{blockedUserId}")
  public ResponseEntity<?> deleteBlock(@PathVariable Long blockedUserId) {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

    blockService.unblockUser(currentUserId, blockedUserId);
    CollectionResponse<UserBlockItem> blockedUsers =
        blockService.getBlockedUserItems(currentUserId);

    return GlobalResponse.ok(blockedUsers);
  }

  @GetMapping
  public ResponseEntity<?> getBlockedUsers() {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

    return GlobalResponse.ok(blockService.getBlockedUserItems(currentUserId));
  }

  @GetMapping("/ids")
  public ResponseEntity<?> getBlockedUserIds() {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

    return GlobalResponse.ok(blockService.getBlockedUserIds(currentUserId));
  }

  @GetMapping("/check/{targetUserId}")
  public ResponseEntity<?> checkBlocked(@PathVariable Long targetUserId) {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

    boolean isBlocked = blockService.isBlocked(currentUserId, targetUserId);
    return GlobalResponse.ok(isBlocked);
  }

  @GetMapping("/mutual-check/{targetUserId}")
  public ResponseEntity<?> checkMutualBlocked(@PathVariable Long targetUserId) {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

    boolean isMutualBlocked = blockService.isMutualBlocked(currentUserId, targetUserId);
    return GlobalResponse.ok(isMutualBlocked);
  }

  @GetMapping("/stats/blocked-by-count")
  public ResponseEntity<?> getBlockedByCount() {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

    long count = blockService.getBlockedByCount(currentUserId);
    return GlobalResponse.ok(count);
  }

  @GetMapping("/stats/blocking-count")
  public ResponseEntity<?> getBlockingCount() {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

    long count = blockService.getBlockingCount(currentUserId);
    return GlobalResponse.ok(count);
  }
}
