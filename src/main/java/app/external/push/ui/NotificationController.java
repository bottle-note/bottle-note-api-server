package app.external.push.ui;

import app.external.push.service.PushHandler;
import app.external.push.service.UserDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/push")
@RequiredArgsConstructor
public class NotificationController {
	private final PushHandler pushHandler;
	private final UserDeviceService deviceService;

	@PostMapping("/token")
	public ResponseEntity<String> saveUserToken(
	) {
		return ResponseEntity.ok("Ok");
	}
}
