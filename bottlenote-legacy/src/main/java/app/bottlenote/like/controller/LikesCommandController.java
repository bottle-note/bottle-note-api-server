package app.bottlenote.like.controller;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;

import app.bottlenote.like.dto.request.LikesUpdateRequest;
import app.bottlenote.like.dto.response.LikesUpdateResponse;
import app.bottlenote.like.service.LikesCommandService;
import app.bottlenote.shared.data.response.GlobalResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/likes")
public class LikesCommandController {

  private final LikesCommandService likesCommandService;

  public LikesCommandController(LikesCommandService likesCommandService) {
    this.likesCommandService = likesCommandService;
  }

  @PutMapping
  public ResponseEntity<?> updateLikes(@Valid @RequestBody LikesUpdateRequest request) {
    Long userId =
        getUserIdByContext()
            .orElseThrow(() -> new UserException(UserExceptionCode.REQUIRED_USER_ID));

    LikesUpdateResponse response =
        likesCommandService.updateLikes(userId, request.reviewId(), request.status());

    return GlobalResponse.ok(response);
  }
}
