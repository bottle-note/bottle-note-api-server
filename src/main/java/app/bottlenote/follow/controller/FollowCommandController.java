package app.bottlenote.follow.controller;

import app.bottlenote.follow.dto.FollowUpdateRequest;
import app.bottlenote.follow.service.FollowCommandService;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;


@RestController
@RequestMapping("/api/v1/follow")
@RequiredArgsConstructor
public class FollowCommandController {

	private final FollowCommandService followCommandService;

	@PostMapping
	public ResponseEntity<GlobalResponse> updateFollowStatus(@RequestBody @Valid FollowUpdateRequest request) {
		Long userId = getUserIdByContext()
			.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));
		return ResponseEntity.ok(GlobalResponse.success(followCommandService.updateFollowStatus(request, userId)));
	}
}
