package app.external.push.ui;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.user.exception.UserException;
import app.external.push.dto.request.TokenSaveRequest;
import app.external.push.service.PushHandler;
import app.external.push.service.UserDeviceService;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static app.bottlenote.global.security.SecurityContextUtil.getUserIdByContext;
import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;

@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
public class NotificationController {
	private final PushHandler pushHandler;
	private final UserDeviceService deviceService;

	@PostMapping("/token")
	public ResponseEntity<?> saveUserToken(
		@RequestBody @Valid TokenSaveRequest request
	) {
		Long userId = getUserIdByContext()
			.orElseThrow(() -> new UserException(REQUIRED_USER_ID));
		return GlobalResponse.ok(deviceService.saveUserToken(userId, request.deviceToken(), request.platform()));
	}

	@GetMapping
	public ResponseEntity<?> sendPush(@RequestParam @Param("msg") String message) {
		pushHandler.sendPush(6L, message);
		return GlobalResponse.ok("");
	}
}
