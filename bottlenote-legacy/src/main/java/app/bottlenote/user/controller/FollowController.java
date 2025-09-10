package app.bottlenote.user.controller;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;
import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.shared.cursor.PageResponse;
import app.bottlenote.shared.data.response.GlobalResponse;
import app.bottlenote.shared.meta.MetaService;
import app.bottlenote.user.dto.request.FollowPageableRequest;
import app.bottlenote.user.dto.request.FollowUpdateRequest;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.service.FollowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/follow")
@RequiredArgsConstructor
public class FollowController {

  private final FollowService followService;

  @GetMapping("/{targetUserId}/following-list")
  public ResponseEntity<?> findFollowingList(
      @PathVariable Long targetUserId, @ModelAttribute FollowPageableRequest pageableRequest) {

    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));

    PageResponse<FollowingSearchResponse> followingPageResponse =
        followService.getFollowingList(currentUserId, targetUserId, pageableRequest);
    return GlobalResponse.ok(
        followingPageResponse.content(),
        MetaService.createMetaInfo().add("pageable", followingPageResponse.cursorPageable()));
  }

  @GetMapping("/{targetUserId}/follower-list")
  public ResponseEntity<?> findFollowerList(
      @PathVariable Long targetUserId, @ModelAttribute FollowPageableRequest pageableRequest) {

    Long currentUserId =
        SecurityContextUtil.getUserIdByContext()
            .orElseThrow(() -> new UserException(REQUIRED_USER_ID));

    PageResponse<FollowerSearchResponse> followerPageResponse =
        followService.getFollowerList(currentUserId, targetUserId, pageableRequest);
    return GlobalResponse.ok(
        followerPageResponse.content(),
        MetaService.createMetaInfo().add("pageable", followerPageResponse.cursorPageable()));
  }

  @PostMapping
  public ResponseEntity<?> updateFollowStatus(@RequestBody @Valid FollowUpdateRequest request) {
    Long userId =
        getUserIdByContext().orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

    return GlobalResponse.ok(followService.updateFollowStatus(request, userId));
  }
}
