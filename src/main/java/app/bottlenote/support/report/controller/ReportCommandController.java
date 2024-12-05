package app.bottlenote.support.report.controller;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.support.report.dto.request.ReviewReportRequest;
import app.bottlenote.support.report.dto.request.UserReportRequest;
import app.bottlenote.support.report.dto.response.UserReportResponse;
import app.bottlenote.support.report.service.ReviewReportService;
import app.bottlenote.support.report.service.UserReportService;
import app.bottlenote.user.exception.UserException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static app.bottlenote.common.support.HttpClient.getClientIP;
import static app.bottlenote.support.report.dto.response.UserReportResponse.UserReportResponseEnum.SAME_USER;
import static app.bottlenote.user.exception.UserExceptionCode.REQUIRED_USER_ID;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/reports")
public class ReportCommandController {

	private final UserReportService userReportService;
	private final ReviewReportService reviewReportService;

	@PostMapping("/user")
	public ResponseEntity<?> reportUser(@RequestBody @Valid UserReportRequest userReportRequest) {

		Long currentUserId = SecurityContextUtil.getUserIdByContext().
			orElseThrow(() -> new UserException(REQUIRED_USER_ID));

		if (currentUserId.equals(userReportRequest.reportUserId())) {
			return ResponseEntity.badRequest()
				.body(GlobalResponse.fail(
					UserReportResponse.of(SAME_USER, null, currentUserId, null)
				));
		}

		return GlobalResponse.ok(
			userReportService.userReport(currentUserId, userReportRequest)
		);
	}

	@PostMapping("/review")
	public ResponseEntity<?> reportReview(
		@RequestBody @Valid ReviewReportRequest reviewReportRequest,
		HttpServletRequest request
	) {
		Long currentUserId = SecurityContextUtil.getUserIdByContext().
			orElseThrow(() -> new UserException(REQUIRED_USER_ID));

		String clientIP = getClientIP(request);

		return GlobalResponse.ok(
			reviewReportService.reviewReport(currentUserId, reviewReportRequest, clientIP)
		);
	}
}
