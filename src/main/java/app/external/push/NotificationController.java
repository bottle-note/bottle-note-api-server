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

	private final PushHandler pushHandler;

	@GetMapping
	public ResponseEntity<String> getNoti(
		@RequestParam(value = "message", defaultValue = "푸시 메시지") String message
	) {
		pushHandler.sendPush(1L, message);
		return ResponseEntity.ok("Ok");
	}

}
