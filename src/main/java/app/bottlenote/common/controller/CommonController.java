package app.bottlenote.common.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/common")
public class CommonController {

	@Value("${server.version}")
	private String serverVersion;

	/**
	 * 서버 상태를 파악하는 API
	 * 비공개 정보입니다 .
	 * globalResponse를 파악하기 위한 엔드포인트 중 하나입니다.
	 */
	@GetMapping("/server-config")
	public ResponseEntity<?> getServerConfig() {
		Map<String, Object> status = new HashMap<>();

		status.put("OS Name", System.getProperty("os.name"));
		status.put("Java Version", System.getProperty("java.version"));
		status.put("Application Version", serverVersion);
		status.put("Application Name", "BottleNote");
		status.put("Server Time", LocalDateTime.now());

		return ResponseEntity.ok(GlobalResponse.success(status));
	}
}
