package app.bottlenote.support.block.controller;

import static app.bottlenote.support.block.exception.BlockExceptionCode.REQUIRED_USER_ID;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.pagination.PageResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.support.block.controller.docs.BlockApiDocs;
import app.bottlenote.support.block.dto.request.BlockCreateRequest;
import app.bottlenote.support.block.dto.request.BlockPageableRequest;
import app.bottlenote.support.block.dto.response.UserBlockListResponse;
import app.bottlenote.support.block.exception.BlockException;
import app.bottlenote.support.block.service.BlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/blocks")
@BlockApiDocs.ApiTag
public class BlockController {

  private final BlockService blockService;

  @BlockApiDocs.CreateBlock
  @PostMapping
  public ResponseEntity<GlobalResponse> createBlock(
      @RequestBody @Valid BlockCreateRequest request) {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

    blockService.blockUser(currentUserId, request.blockedUserId());
    return okBlockedUsers(currentUserId, new BlockPageableRequest(null, null));
  }

  @BlockApiDocs.DeleteBlock
  @DeleteMapping("/{blockedUserId}")
  public ResponseEntity<GlobalResponse> deleteBlock(@PathVariable Long blockedUserId) {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

    blockService.unblockUser(currentUserId, blockedUserId);
    return okBlockedUsers(currentUserId, new BlockPageableRequest(null, null));
  }

  @BlockApiDocs.GetBlockedUsers
  @GetMapping
  public ResponseEntity<GlobalResponse> getBlockedUsers(
      @ModelAttribute @Valid BlockPageableRequest request) {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));
    return okBlockedUsers(currentUserId, request);
  }

  private ResponseEntity<GlobalResponse> okBlockedUsers(
      Long currentUserId, BlockPageableRequest request) {
    PageResponse<UserBlockListResponse> page =
        blockService.getBlockedUserItems(currentUserId, request);
    return GlobalResponse.ok(
        page.content(), MetaService.createMetaInfo().add("pagination", page.pagination()));
  }

  @BlockApiDocs.GetBlockedUserIds
  @GetMapping("/ids")
  public ResponseEntity<GlobalResponse> getBlockedUserIds() {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

    return GlobalResponse.ok(blockService.getBlockedUserIds(currentUserId));
  }

  @BlockApiDocs.CheckBlocked
  @GetMapping("/check/{targetUserId}")
  public ResponseEntity<GlobalResponse> checkBlocked(@PathVariable Long targetUserId) {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

    boolean isBlocked = blockService.isBlocked(currentUserId, targetUserId);
    return GlobalResponse.ok(isBlocked);
  }

  @BlockApiDocs.CheckMutualBlocked
  @GetMapping("/mutual-check/{targetUserId}")
  public ResponseEntity<GlobalResponse> checkMutualBlocked(@PathVariable Long targetUserId) {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

    boolean isMutualBlocked = blockService.isMutualBlocked(currentUserId, targetUserId);
    return GlobalResponse.ok(isMutualBlocked);
  }

  @BlockApiDocs.GetBlockedByCount
  @GetMapping("/stats/blocked-by-count")
  public ResponseEntity<GlobalResponse> getBlockedByCount() {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

    long count = blockService.getBlockedByCount(currentUserId);
    return GlobalResponse.ok(count);
  }

  @BlockApiDocs.GetBlockingCount
  @GetMapping("/stats/blocking-count")
  public ResponseEntity<GlobalResponse> getBlockingCount() {
    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new BlockException(REQUIRED_USER_ID));

    long count = blockService.getBlockingCount(currentUserId);
    return GlobalResponse.ok(count);
  }
}
