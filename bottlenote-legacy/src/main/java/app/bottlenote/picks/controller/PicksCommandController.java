package app.bottlenote.picks.controller;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;

import app.bottlenote.picks.dto.request.PicksUpdateRequest;
import app.bottlenote.picks.service.PicksCommandService;
import app.bottlenote.shared.data.response.GlobalResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/picks")
@RequiredArgsConstructor
public class PicksCommandController {

  private final PicksCommandService picksCommandService;

  @PutMapping
  public ResponseEntity<GlobalResponse> updatePicks(
      @RequestBody @Valid PicksUpdateRequest request) {

    Long userId =
        getUserIdByContext()
            .orElseThrow(() -> new UserException(UserExceptionCode.REQUIRED_USER_ID));

    return ResponseEntity.ok(
        GlobalResponse.success(picksCommandService.updatePicks(request, userId)));
  }
}
