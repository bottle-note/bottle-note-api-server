package app.bottlenote.follow.controller;

import app.bottlenote.follow.dto.request.FollowUpdateRequest;
import app.bottlenote.follow.dto.request.FollowPageableRequest;
import app.bottlenote.follow.dto.response.FollowSearchResponse;
import app.bottlenote.follow.service.FollowCommandService;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static app.bottlenote.global.data.response.GlobalResponse.success;
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

		return ResponseEntity.ok(success(followCommandService.updateFollowStatus(request, userId)));
	}

	@GetMapping("/{userId}/follower")
	public ResponseEntity<GlobalResponse> findFollowerList(@PathVariable Long userId, @ModelAttribute FollowPageableRequest pageableRequest) {

		PageResponse<FollowSearchResponse> pageResponse = followCommandService.findFollowerList(userId, pageableRequest);

		return ResponseEntity.ok(
			GlobalResponse.success(
				pageResponse.content(),
				MetaService.createMetaInfo().add("pageable", pageResponse.cursorPageable())
			)
		);
	}

	@GetMapping("/{userId}/follow")
	public ResponseEntity<GlobalResponse> findFollowList(@PathVariable Long userId, @ModelAttribute FollowPageableRequest pageableRequest) {

		PageResponse<FollowSearchResponse> pageResponse = followCommandService.findFollowList(userId, pageableRequest);

		return ResponseEntity.ok(
			GlobalResponse.success(
				pageResponse.content(),
				MetaService.createMetaInfo().add("pageable", pageResponse.cursorPageable())
			)
		);
	}

}
