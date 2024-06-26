package app.bottlenote.follow.controller;

import app.bottlenote.follow.dto.request.FollowPageableRequest;
import app.bottlenote.follow.dto.request.FollowUpdateRequest;
import app.bottlenote.follow.dto.response.FollowSearchResponse;
import app.bottlenote.follow.service.FollowService;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.global.service.meta.MetaService;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import static app.bottlenote.global.data.response.GlobalResponse.success;
import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;

@RestController
@RequestMapping("/api/v1/follow")
@RequiredArgsConstructor
public class FollowController {

	private final FollowService followService;

	@GetMapping("/{userId}")
	public ResponseEntity<GlobalResponse> findFollowList(@PathVariable Long userId, @ModelAttribute FollowPageableRequest pageableRequest) {

		PageResponse<FollowSearchResponse> pageResponse = followService.findFollowList(userId, pageableRequest);

		return ResponseEntity.ok(
			GlobalResponse.success(
				pageResponse.content(),
				MetaService.createMetaInfo().add("pageable", pageResponse.cursorPageable())
			)
		);
	}

	@PostMapping
	public ResponseEntity<GlobalResponse> updateFollowStatus(@RequestBody @Valid FollowUpdateRequest request) {
		Long userId = getUserIdByContext()
			.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

		return ResponseEntity.ok(success(followService.updateFollowStatus(request, userId)));
	}



}
