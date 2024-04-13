package app.bottlenote.support.report.service;

import app.bottlenote.support.report.dto.request.UserReportRequest;
import app.bottlenote.support.report.repository.UserReportRepository;
import app.bottlenote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserReportService {

	private final UserReportRepository userReportRepository;
	private final UserRepository userRepository;

	public void userReport(UserReportRequest userReportRequest) {
		//	log.info("UserReportService.reportUser");
		log.info("신고자 정보: {}", userReportRequest.toString());
	}
}
