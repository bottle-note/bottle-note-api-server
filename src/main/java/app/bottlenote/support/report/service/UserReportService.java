package app.bottlenote.support.report.service;

import app.bottlenote.support.report.dto.request.UserReportRequest;
import app.bottlenote.support.report.dto.response.UserReportResponse;
import app.bottlenote.support.report.repository.UserReportRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
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

	public UserReportResponse userReport(UserReportRequest userReportRequest) {

		User reportUser = userRepository.findById(userReportRequest.reportUserId())
			.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));


		log.info("신고자 정보: {}", userReportRequest.toString());
		return null;
	}
}
