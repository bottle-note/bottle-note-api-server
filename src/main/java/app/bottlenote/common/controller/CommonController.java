package app.bottlenote.common.controller;

import app.bottlenote.common.dto.request.RestdocsRequest;
import app.bottlenote.common.dto.response.RestdocsResponse;
import app.bottlenote.common.service.CommonService;
import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.exception.custom.CustomException;
import app.bottlenote.global.exception.custom.code.CustomExceptionCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/common")
public class CommonController {

	private final CommonService commonService;

	@GetMapping
	public ResponseEntity<GlobalResponse> getServerInfo() {
		return ResponseEntity.ok(GlobalResponse.success(Map.of("message", "Server is running")));
	}

	/**
	 * error test를 위한 controller
	 *
	 * @return the server error
	 */
	@GetMapping("/error")
	public ResponseEntity<GlobalResponse> getServerError() {
		throw new CustomException(CustomExceptionCode.INTERNAL_SERVER_ERROR);
	}

	@GetMapping("/rest-docs")
	public ResponseEntity<GlobalResponse> getRestDocs(@RequestBody RestdocsRequest request) {
		LocalDateTime restdocs = commonService.restdocs();
		return ResponseEntity
			.ok(GlobalResponse.success(new RestdocsResponse(restdocs)));
	}
}
