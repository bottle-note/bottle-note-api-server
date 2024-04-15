package app.bottlenote.support.report.service;

import app.bottlenote.support.constant.StatusType;
import app.bottlenote.support.report.domain.UserReports;
import app.bottlenote.support.report.dto.request.UserReportRequest;
import app.bottlenote.support.report.dto.response.UserReportResponse;
import app.bottlenote.support.report.exception.ReportException;
import app.bottlenote.support.report.repository.UserReportRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static app.bottlenote.support.report.dto.response.UserReportResponse.UserReportResponseEnum.SUCCESS;
import static app.bottlenote.support.report.exception.ReportExceptionCode.REPORT_CONTENT_OVERFLOW;
import static app.bottlenote.support.report.exception.ReportExceptionCode.REPORT_LIMIT_EXCEEDED;
import static app.bottlenote.support.report.exception.ReportExceptionCode.REPORT_USER_NOT_FOUND;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserReportService {

	private final UserReportRepository userReportRepository;
	private final UserRepository userRepository;

	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	public UserReportResponse userReport(UserReportRequest userReportRequest) {
		User user = userRepository.findById(userReportRequest.userId())
			.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

		//신고 대상 사용자가 존재하는지 확인
		User reportUser = userRepository.findById(userReportRequest.reportUserId())
			.orElseThrow(() -> new ReportException(REPORT_USER_NOT_FOUND));

		//신고 대상 사용자에 대한 신고 내용이 적절한지 확인 ( ex. 100자 이내 )
		String content = isValidContent(userReportRequest.content());

		LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
		List<UserReports> userReportsList = userReportRepository.findByUserIdAndCreateAtAfter(user.getId(), today);

		//신고 대상 사용자가 이미 신고된 사용자인지 확인
		userReportsList.stream()
			.filter(report -> report.getReportUser().getId().equals(userReportRequest.reportUserId()))
			.findFirst()
			.ifPresent(report -> {
				log.warn("이미 신고한 사용자입니다. 신고자: {}({}), 신고 대상: {}({})", user.getNickName(), user.getId(), reportUser.getNickName(), reportUser.getId());
				throw new ReportException(REPORT_LIMIT_EXCEEDED);
			});

		//신고 대상 사용자가 일일 신고 횟수 제한을 초과하는지 확인
		if (userReportsList.size() >= 5) {
			log.warn("신고 대상 사용자({}:{})가 일일 신고 횟수 제한을 초과했습니다.", reportUser.getId(), reportUser.getNickName());
			throw new ReportException(REPORT_LIMIT_EXCEEDED);
		}

		UserReports reports = UserReports.builder()
			.user(user)
			.reportUser(reportUser)
			.type(userReportRequest.type())
			.status(StatusType.WAITING)
			.content(content)
			.adminId(1L)
			.build();

		UserReports saveReport = userReportRepository.save(reports);

		log.info("신고자 정보: {}", saveReport.getUser().getId() + saveReport.getUser().getNickName());

		return UserReportResponse.of(SUCCESS, saveReport.getId(), saveReport.getUser().getNickName());
	}

	private String isValidContent(String content) {
		if (content.length() > 300) {
			throw new ReportException(REPORT_CONTENT_OVERFLOW);
		}
		//todo 부적절한 단어 filter ( 욕설, 스팸등의 필터링 추후 util 클래스 등으로 관리할 수 있음.)

		return content;
	}
}
