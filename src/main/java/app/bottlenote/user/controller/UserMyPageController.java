package app.bottlenote.user.controller;

import app.bottlenote.global.annotation.AccessPolicy;
import app.bottlenote.global.annotation.AccessPolicy.AccessType;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.dto.request.MyBottleRequest;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.service.UserBasicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/my-page")
public class UserMyPageController {

	private final UserBasicService userBasicService;

	/**
	 * 마이 페이지 조회 API
	 * 모든 유저가 조회 가능
	 */
	@AccessPolicy(type = AccessType.ALL)
	@GetMapping("/{userId}")
	public ResponseEntity<?> getMyPage(@PathVariable Long userId) {
		final Long currentUserId = SecurityContextUtil.getUserIdByContext().orElse(-1L);
		return GlobalResponse.ok(userBasicService.getMyPage(userId, currentUserId));
	}

	/**
	 * 마이 보틀 노트 조회 API
	 * 본인만 조회 가능
	 */
	@AccessPolicy(type = AccessType.OWNER)
	@GetMapping("/{userId}/my-bottle")
	public ResponseEntity<?> getMyBottle(
		@PathVariable(name = "userId") Long userId,
		@ModelAttribute(name = "myBottleRequest") MyBottleRequest myBottleRequest
	) {
		final Long currentUserId = SecurityContextUtil.getUserIdByContext()
			.orElseThrow(() -> new UserException(REQUIRED_USER_ID));
		return GlobalResponse.ok(userBasicService.getMyBottle(userId, currentUserId, myBottleRequest));
	}
}
