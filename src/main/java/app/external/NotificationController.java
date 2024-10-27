package app.external;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

		//c1tAFlaFZEAjuRu9lBNMK-:APA91bF0qzvm9ChbmFOiK-PbXVDehf6vSWoguekCLzcj3aByResbjV8enCGRod06g7WT0UsmZLVMYNt24Z8qy4nb9ZZXCdhXvD-cLReogIaZ6RhfX0IMWgf4JlOvmGBpdRcWUFbLGqyu
		String fcmTestTitle = fcmTriggerService.sendMessage(fcmToken, "FCM Test Title", message);

		return ResponseEntity.ok(fcmTestTitle);
	}

}
