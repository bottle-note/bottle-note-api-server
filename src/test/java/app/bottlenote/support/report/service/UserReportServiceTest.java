package app.bottlenote.support.report.service;

import app.bottlenote.support.report.domain.UserReports;
import app.bottlenote.support.report.domain.constant.UserReportType;
import app.bottlenote.support.report.dto.request.UserReportRequest;
import app.bottlenote.support.report.dto.response.UserReportResponse;
import app.bottlenote.support.report.exception.ReportException;
import app.bottlenote.support.report.exception.ReportExceptionCode;
import app.bottlenote.support.report.repository.UserReportRepository;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.exception.UserException;
import app.bottlenote.user.exception.UserExceptionCode;
import app.bottlenote.user.repository.UserCommandRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static app.bottlenote.support.constant.StatusType.WAITING;
import static app.bottlenote.user.domain.constant.SocialType.GOOGLE;
import static app.bottlenote.user.domain.constant.UserType.ROLE_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("[unit] [service] UserReportService")
@ExtendWith(MockitoExtension.class)
class UserReportServiceTest {

	@InjectMocks
	private UserReportService userReportService;

	@Mock
	private UserReportRepository userReportRepository;

	@Mock
	private UserCommandRepository userRepository;

	@Test
	@DisplayName("유저 아이디가 없을 경우  NullPointerException 발생한다.")
	void testUserReportWith_NullUserId() {
		// Given
		final Long currentUser = null;
		UserReportRequest request = new UserReportRequest(44L, UserReportType.SPAM, "content_request_content_01");

		// When & Then
		NullPointerException aThrows = assertThrows(NullPointerException.class, () -> userReportService.userReport(currentUser, request));
		assertEquals(aThrows.getMessage(), "유저 ID는 필수 값입니다.");
		System.out.println(aThrows.getMessage());
	}

	@Test
	@DisplayName("신고 대상자 아이디가 없을 경우 NullPointerException이 발생한다.")
	void testUserReportWith_NullReportUserId() {
		// Given
		final Long currentUser = 1L;

		UserReportRequest request = new UserReportRequest(null, UserReportType.SPAM, "content_request_content_01");

		// When & Then
		NullPointerException aThrows = assertThrows(NullPointerException.class, () -> userReportService.userReport(currentUser, request));
		assertEquals(aThrows.getMessage(), "신고 대상자 ID는 필수 값입니다.");
		System.out.println(aThrows.getMessage());
	}

	@Test
	@DisplayName("유저 아이디와 신고 대상자 아이디가 같을 경우 SELF_REPORT 예외가 발생한다.")
	void testUserReportWith_SelfReport() {
		// Given
		UserReportRequest request = new UserReportRequest(1L, UserReportType.SPAM, "content_request_content_01");

		// When & Then
		ReportException aThrows = assertThrows(ReportException.class, () -> userReportService.userReport(1L, request));
		assertEquals(aThrows.getExceptionCode(), ReportExceptionCode.SELF_REPORT);
		assertEquals(aThrows.getMessage(), ReportExceptionCode.SELF_REPORT.getMessage());
		System.out.println(aThrows.getExceptionCode());
	}

	@Test
	@DisplayName("유저를 신고 할 수 있다.")
	void testUserReport() {
		// Given
		User user_1 = User.builder().id(1L).nickName("사용자").email("test_01@test.co.kr").role(ROLE_USER).socialType(new ArrayList<>(List.of(GOOGLE))).build();
		User reportUser_1 = User.builder().id(2L).nickName("신고대상자_1").email("report_user_01@test.co.kr").role(ROLE_USER).socialType(new ArrayList<>(List.of(GOOGLE))).build();
		UserReports reports_1 = UserReports.builder().id(1L).user(user_1).reportUser(reportUser_1).content("test_01").status(WAITING).build();
		List<UserReports> userReportsList = List.of(reports_1);

		UserReportRequest request = new UserReportRequest(4L, UserReportType.SPAM, "content_request_content_01");

		// When
		when(userRepository.findById(any(Long.class))).thenReturn(Optional.of(user_1)); // 사용자 조회
		when(userRepository.findById(any(Long.class))).thenReturn(Optional.of(reportUser_1)); // 사용자, 신고자 조회
		when(userReportRepository.findByUserIdAndCreateAtAfter(any(Long.class), any(LocalDateTime.class))).thenReturn(userReportsList); // 사용자의 신고 목록 조회
		when(userReportRepository.save(any(UserReports.class))).thenReturn(reports_1);  // 신고 등록

		UserReportResponse response = userReportService.userReport(1L, request);

		// Then
		assertEquals(response.getReportId(), reports_1.getId());
		assertEquals(response.getReportUserId(), reportUser_1.getId());
		assertEquals(response.getReportUserName(), reportUser_1.getNickName());
		assertEquals(response.getMessage(), UserReportResponse.UserReportResponseEnum.SUCCESS.getMessage());
	}

	@Test
	@DisplayName("신고자가 존재하지 않는 경우 UserException이 발생한다.")
	void testUserReportWith_UserNotFoundException() {
		// Given
		UserReportRequest request = new UserReportRequest(44L, UserReportType.SPAM, "content_request_content_01");

		// When
		when(userRepository.findById(any(Long.class))).thenReturn(Optional.empty()); // 사용자 조회

		// Then
		UserException aThrows = assertThrows(UserException.class, () -> userReportService.userReport(1L, request));
		assertEquals(aThrows.getExceptionCode(), UserExceptionCode.USER_NOT_FOUND);
		assertEquals(aThrows.getMessage(), UserExceptionCode.USER_NOT_FOUND.getMessage());
		System.out.println(aThrows.getExceptionCode());
	}

	@Test
	@DisplayName("신고 대상자가 존재하지 않는 경우 user exception이 아닌 report exception이 발생한다.")
	void testUserReportWith_ReportUserNotFoundException() {
		// Given
		UserReportRequest request = new UserReportRequest(44L, UserReportType.SPAM, "content_request_content_01");

		User user = User.builder().id(1L).nickName("사용자").email("test_01@test.co.kr").role(ROLE_USER).socialType(new ArrayList<>(List.of(GOOGLE))).build();

		// When
		when(userRepository.findById(1L)).thenReturn(Optional.of(user)); // 신고자 조회
		when(userRepository.findById(44L)).thenReturn(Optional.empty()); // 신고 대상자 조회

		// Then
		ReportException aThrows = assertThrows(ReportException.class, () -> userReportService.userReport(1L, request));
		assertEquals(aThrows.getExceptionCode(), ReportExceptionCode.REPORT_USER_NOT_FOUND);
		assertEquals(aThrows.getMessage(), ReportExceptionCode.REPORT_USER_NOT_FOUND.getMessage());
		System.out.println(aThrows.getExceptionCode());
	}

	@Test
	@DisplayName("해당 유저를 이미 신고한 경우 ALREADY_REPORTED_USER 예외가 발생한다.")
	void testUserReportWith_ALREADY_REPORTED_USER() {
		// Given
		UserReportRequest request = new UserReportRequest(44L, UserReportType.SPAM, "content_request_content_01");

		User user = User.builder().id(1L).nickName("사용자").email("test_01@test.co.kr").role(ROLE_USER).socialType(new ArrayList<>(List.of(GOOGLE))).build();
		User reportUser = User.builder().id(44L).nickName("신고대상자_1").email("report_user_01@test.co.kr").role(ROLE_USER).socialType(new ArrayList<>(List.of(GOOGLE))).build();
		List<UserReports> userReportsList = List.of(
			UserReports.builder().id(1L).user(user).reportUser(reportUser).content("test_01").status(WAITING).build(),
			UserReports.builder().id(2L).user(user).reportUser(reportUser).content("test_02").status(WAITING).build(),
			UserReports.builder().id(3L).user(user).reportUser(reportUser).content("test_03").status(WAITING).build(),
			UserReports.builder().id(4L).user(user).reportUser(reportUser).content("test_04").status(WAITING).build(),
			UserReports.builder().id(5L).user(user).reportUser(reportUser).content("test_05").status(WAITING).build()
		);

		// When & Then
		ReportException aThrows = assertThrows(ReportException.class, () -> userReportService.validateReportRequest(request, userReportsList, user, reportUser));
		assertEquals(aThrows.getExceptionCode(), ReportExceptionCode.ALREADY_REPORTED_USER);
		assertEquals(aThrows.getMessage(), ReportExceptionCode.ALREADY_REPORTED_USER.getMessage());
	}

	@Test
	@DisplayName("해당 유저의 일일 조회 수가 신고 제한 횟수를 초과했을 때 REPORT_LIMIT_EXCEEDED 예외가 발생한다.")
	void testUserReportWith_REPORT_LIMIT_EXCEEDED() {
		// Given
		UserReportRequest request = new UserReportRequest(43L, UserReportType.SPAM, "content_request_content_01");
		User user = User.builder().id(1L).nickName("사용자").email("test_01@test.co.kr").role(ROLE_USER).socialType(new ArrayList<>(List.of(GOOGLE))).build();
		User reportUser_00 = User.builder().id(43L).nickName("신고대상자_1").email("report01@email.com").build();
		User reportUser_01 = User.builder().id(44L).nickName("신고대상자_1").email("report01@email.com").build();
		User reportUser_02 = User.builder().id(44L).nickName("신고대상자_1").email("report02@email.com").build();
		User reportUser_03 = User.builder().id(44L).nickName("신고대상자_1").email("report03@email.com").build();
		User reportUser_04 = User.builder().id(44L).nickName("신고대상자_1").email("report04@email.com").build();
		User reportUser_05 = User.builder().id(44L).nickName("신고대상자_1").email("report05@email.com").build();

		List<UserReports> userReportsList = List.of(
			UserReports.builder().id(1L).user(user).reportUser(reportUser_01).content("test_01").status(WAITING).build(),
			UserReports.builder().id(2L).user(user).reportUser(reportUser_02).content("test_02").status(WAITING).build(),
			UserReports.builder().id(3L).user(user).reportUser(reportUser_03).content("test_03").status(WAITING).build(),
			UserReports.builder().id(4L).user(user).reportUser(reportUser_04).content("test_04").status(WAITING).build(),
			UserReports.builder().id(5L).user(user).reportUser(reportUser_05).content("test_05").status(WAITING).build()
		);

		// When & Then
		ReportException aThrows = assertThrows(ReportException.class, () -> userReportService.validateReportRequest(request, userReportsList, user, reportUser_00));
		assertEquals(aThrows.getExceptionCode(), ReportExceptionCode.REPORT_LIMIT_EXCEEDED);
		assertEquals(aThrows.getMessage(), ReportExceptionCode.REPORT_LIMIT_EXCEEDED.getMessage());
	}
}
