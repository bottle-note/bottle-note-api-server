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

	private final static Integer REPORT_LIMIT = 5;

	/**
	 * 사용자의 신고를 등록하는 메소드입니다.
	 *
	 * @param userReportRequest the user report request
	 * @return the user report response
	 */
	@Transactional
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	public UserReportResponse userReport(UserReportRequest userReportRequest) {

		User user = userRepository.findById(userReportRequest.userId())
			.orElseThrow(() -> new UserException(UserExceptionCode.USER_NOT_FOUND));

		User reportUser = userRepository.findById(userReportRequest.reportUserId())
			.orElseThrow(() -> new ReportException(REPORT_USER_NOT_FOUND));


		LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
		List<UserReports> userReportsList = userReportRepository.findByUserIdAndCreateAtAfter(user.getId(), today);

		validateReportRequest(userReportRequest, userReportsList, user, reportUser);

		String content = checkVerificationContent(userReportRequest.content());

		UserReports reports = UserReports.builder()
			.user(user)
			.reportUser(reportUser)
			.type(userReportRequest.type())
			.status(StatusType.WAITING)
			.content(content)
			.adminId(1L)
			.build();

		UserReports saveReport = userReportRepository.save(reports);

		log.info("신고 처리 완료 :: 신고자 정보: {}", saveReport.getUser().getId() + saveReport.getUser().getNickName());
		return UserReportResponse.of(SUCCESS, saveReport.getId(), user.getId(), user.getNickName());
	}

	/**
	 * 해당 메소드는 유저 신고에 대한 비지니스 유효성 검사를 수행합
	 * 예를 들어 사용자 신고 대상이 이미 신고된 사용자인지, 일일 신고 횟수 제한을 초과하는지 등을 검사합니다.
	 *
	 * @param userReportRequest the user report request
	 * @param userReportsList   the user reports list
	 * @param user              the user
	 * @param reportUser        the report user
	 * @throws ReportException the report exception
	 */
	@Transactional
	protected void validateReportRequest(UserReportRequest userReportRequest, List<UserReports> userReportsList, User user, User reportUser) throws ReportException {
		//신고 대상 사용자가 이미 신고된 사용자인지 확인
		userReportsList.stream()
			.filter(report -> report.getReportUser().getId().equals(userReportRequest.reportUserId()))
			.findFirst()
			.ifPresent(report -> {
				log.warn("이미 신고한 사용자입니다. 신고자: {}({}), 신고 대상: {}({})", user.getNickName(), user.getId(), reportUser.getNickName(), reportUser.getId());
				throw new ReportException(REPORT_LIMIT_EXCEEDED);
			});

		//신고 대상 사용자가 일일 신고 횟수 제한을 초과하는지 확인
		if (userReportsList.size() >= REPORT_LIMIT) {
			log.warn("신고 대상 사용자({}:{})가 일일 신고 횟수 제한을 초과했습니다.", reportUser.getId(), reportUser.getNickName());
			throw new ReportException(REPORT_LIMIT_EXCEEDED);
		}
	}

	/**
	 * 해당 메소드는 신고 내용에 대한 유효성 검사를 수행합니다.
	 * 이 검증 절차에는 신고 내용의 길이가 300자를 초과하는지, 부적절한 단어가 포함되어 있는지 등을 확인합니다.
	 * 다만 현재 정의 되지 않은 정책은 추후 정의될 수 있습니다.
	 *
	 * @param content the content
	 * @return the string
	 */
	private String checkVerificationContent(String content) {
		if (content.length() > 300) {
			throw new ReportException(REPORT_CONTENT_OVERFLOW);
		}

		return content;
	}
}
