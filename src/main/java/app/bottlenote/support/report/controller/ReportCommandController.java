package app.bottlenote.support.report.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.support.report.dto.request.UserReportRequest;
import app.bottlenote.support.report.dto.response.UserReportResponse;
import app.bottlenote.support.report.service.UserReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static app.bottlenote.support.report.dto.response.UserReportResponse.UserReportResponseEnum.SAME_USER;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/reports")
public class ReportCommandController {

	private final UserReportService userReportService;

	@PostMapping("/user")
	public ResponseEntity<GlobalResponse> reportUser(@RequestBody @Valid UserReportRequest userReportRequest) {

		if (userReportRequest.userId().equals(userReportRequest.reportUserId())) {
			return ResponseEntity.badRequest()
				.body(GlobalResponse.fail(
					UserReportResponse.of(SAME_USER, userReportRequest.userId(), null)
				));
		}

		return ResponseEntity.ok(
			GlobalResponse.success(
				userReportService.userReport(userReportRequest)
			));
	}

}
