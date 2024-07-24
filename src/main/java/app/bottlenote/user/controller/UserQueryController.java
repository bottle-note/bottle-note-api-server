package app.bottlenote.user.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserQueryController {

	private final UserService userQueryService;

	@GetMapping("/my-page/{userId}")
	public ResponseEntity<GlobalResponse> searchMyPage(@PathVariable Long userId) {

		Long currentUserId =
			SecurityContextUtil.getUserIdByContext().orElse(-1L);

		return ResponseEntity.ok(
			GlobalResponse.success(
				userQueryService.searchMyPage(userId, currentUserId)
			));
	}
}
