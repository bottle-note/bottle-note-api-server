package app.bottlenote.user.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserQueryController {

	private final UserQueryService userQueryService;

	@GetMapping("mypage/{userId}")
	public ResponseEntity<?> getMypage(Long userId) {

		Long currentUserId = SecurityContextUtil.getUserIdByContext().orElse(-1L);

		MyPageResponse mypage = userQueryService.getMypage(userId, currentUserId);

		return ResponseEntity.ok(GlobalResponse.success(mypage));
	}

}
