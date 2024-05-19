package app.bottlenote.follow.controller;

import app.bottlenote.follow.dto.FollowUpdateRequest;
import app.bottlenote.follow.dto.FollowUpdateResponse;
import app.bottlenote.follow.service.FollowCommandService;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static app.bottlenote.global.security.SecurityUtil.getCurrentUserId;

@RestController
@RequestMapping("/api/v1/follow")
@RequiredArgsConstructor
public class FollowCommandController {

	private final FollowCommandService followCommandService;

	@PutMapping
	public ResponseEntity<GlobalResponse> updateFollow( @RequestBody @Valid FollowUpdateRequest request) {

		Long userId = getCurrentUserId();
		if( userId == null) {
			throw new UserException(UserExceptionCode.REQUIRED_USER_ID);
		}

		return ResponseEntity.ok(
			GlobalResponse.success(
				followCommandService.updateFollow(request, userId)
			)
		);
	}

}
