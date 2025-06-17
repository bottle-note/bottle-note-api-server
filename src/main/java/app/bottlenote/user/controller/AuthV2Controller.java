package app.bottlenote.user.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/auth")
public class AuthV2Controller {
	private final AuthService authService;

	@GetMapping("/admin/permissions")
	public ResponseEntity<?> checkAdminStatus() {
		Long currentUserId = SecurityContextUtil.getUserIdByContext().
				orElseThrow(() -> new UserException(REQUIRED_USER_ID));

		boolean is = authService.checkAdminStatus(currentUserId);
		return GlobalResponse.ok(is);
	}
}
