package app.bottlenote.support.report.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.support.report.service.UserReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/reports")
public class ReportCommandController {

	private final UserReportService userReportService;

	@PostMapping("/user")
	public ResponseEntity<GlobalResponse> reportUser() {
		userReportService.userReport();
		return ResponseEntity.ok(GlobalResponse.success("신고 성공"));
	}

}
