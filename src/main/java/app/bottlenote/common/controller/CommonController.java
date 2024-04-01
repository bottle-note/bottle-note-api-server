package app.bottlenote.common.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.exception.custom.CustomException;
import app.bottlenote.global.exception.custom.code.CustomExceptionCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/common")
public class CommonController {

	@GetMapping
	public ResponseEntity<?> getServerInfo() {
		return ResponseEntity.ok(GlobalResponse.success(Map.of("message", "Server is running")));
	}

	/**
	 * error test를 위한 controller
	 *
	 * @return the server error
	 */
	@GetMapping("/error")
	public ResponseEntity<?> getServerError() {
		throw new CustomException(CustomExceptionCode.INTERNAL_SERVER_ERROR);
	}
}
