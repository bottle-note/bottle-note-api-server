package app.bottlenote.user.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.dto.request.MyBottleRequest;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.service.UserQueryService;
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
@RequestMapping("/api/v1/mypage")
public class UserQueryController {

	private final UserQueryService userQueryService;

	@GetMapping("/{userId}")
	public ResponseEntity<?> getMypage(@PathVariable Long userId) {

		Long currentUserId = SecurityContextUtil.getUserIdByContext().orElse(-1L);

		MyPageResponse mypage = userQueryService.getMypage(userId, currentUserId);

		return GlobalResponse.ok(mypage);
	}

	@GetMapping("/{userId}/my-bottle")
	public ResponseEntity<?> getMyBottle(@PathVariable Long userId, @ModelAttribute MyBottleRequest myBottleRequest) {

		Long currentUserId = SecurityContextUtil.getUserIdByContext().orElseThrow(
			() -> new UserException(REQUIRED_USER_ID)
		);
		MyBottleResponse myBottle = userQueryService.getMyBottle(userId, currentUserId, myBottleRequest);

		return GlobalResponse.ok(myBottle);
	}

}
