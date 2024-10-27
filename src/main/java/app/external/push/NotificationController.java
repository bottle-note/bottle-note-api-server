package app.external.push;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Push test controller ( delete after test )
 */
@RestController
@RequestMapping("/noti")
@RequiredArgsConstructor
public class NotificationController {

	private final FcmTriggerService fcmTriggerService;

	@GetMapping
	public ResponseEntity<?> getNoti(
		@RequestParam(value = "fcmToken", defaultValue = "null") String fcmToken,
		@RequestParam(value = "message", defaultValue = "푸시 메시지") String message
	) {
		String fcmTestTitle = fcmTriggerService.sendMessage(fcmToken, "FCM Test Title", message);

		return ResponseEntity.ok(fcmTestTitle);
	}

}
