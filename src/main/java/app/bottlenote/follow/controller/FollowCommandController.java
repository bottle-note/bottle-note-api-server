package app.bottlenote.follow.controller;

import app.bottlenote.follow.dto.FollowUpdateRequest;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.follow.service.FollowCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static app.bottlenote.global.security.SecurityUtil.getCurrentUserId;

@RestController
@RequestMapping("/api/v1/follow")
@RequiredArgsConstructor
public class FollowCommandController {

	private final FollowCommandService followCommandService;

	@PostMapping
	public ResponseEntity<GlobalResponse> updateFollowStatus(@RequestBody @Valid FollowUpdateRequest request) {
		Long userId = getCurrentUserId();
		return ResponseEntity.ok(GlobalResponse.success(followCommandService.updateFollowStatus(request, userId)));
	}
}
