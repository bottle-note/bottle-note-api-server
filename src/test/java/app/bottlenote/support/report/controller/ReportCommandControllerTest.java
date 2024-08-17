package app.bottlenote.support.report.controller;

import app.bottlenote.global.data.response.Error;
import app.bottlenote.global.exception.custom.code.ValidExceptionCode;
import app.bottlenote.support.report.domain.constant.UserReportType;
import app.bottlenote.support.report.dto.request.UserReportRequest;
import app.bottlenote.support.report.dto.response.UserReportResponse;
import app.bottlenote.support.report.service.UserReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static app.bottlenote.support.report.dto.response.UserReportResponse.UserReportResponseEnum.SAME_USER;
import static app.bottlenote.support.report.dto.response.UserReportResponse.UserReportResponseEnum.SUCCESS;
import static app.bottlenote.support.report.dto.response.UserReportResponse.of;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Tag("unit")
@DisplayName("[unit] [controller] ReportCommandController")
@WebMvcTest(ReportCommandController.class)
@WithMockUser //인증된 사용자로 설정
class ReportCommandControllerTest {

	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@MockBean
	private UserReportService userReportService;

	@DisplayName("유저를 신고 할 수 있다.")
	@Test
	void reportUserTest() throws Exception {
		// given
		UserReportRequest request = new UserReportRequest(1L, 2L, UserReportType.OTHER, "신고 내용 쏼라 쏼라 쏼라 ");
		UserReportResponse response = of(SUCCESS, 1L, 2L, "신고한 유저 이름");

		// when
		when(userReportService.userReport(request)).thenReturn(response);

		// then
		mockMvc.perform(post("/api/v1/reports/user")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content(mapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value("true"))
			.andExpect(jsonPath("$.code").value("200"))
			.andExpect(jsonPath("$.data.message").value(SUCCESS.getMessage()))
			.andExpect(jsonPath("$.data.reportId").value(response.getReportId()))
			.andExpect(jsonPath("$.data.reportUserId").value(response.getReportUserId()))
			.andExpect(jsonPath("$.data.reportUserName").value(response.getReportUserName()))
			.andDo(print());
	}

	@DisplayName("자기 자신을 신고 할 수 없다.")
	@Test
	void cantNotReportMySelf() throws Exception {
		// given
		UserReportRequest request = new UserReportRequest(1L, 1L, UserReportType.OTHER, "신고 내용 쏼라 쏼라 쏼라 ");
		UserReportResponse response = UserReportResponse.of(SAME_USER, null, request.userId(), null);
		// then
		mockMvc.perform(post("/api/v1/reports/user")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content(mapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value("false"))
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.data").isEmpty())
			.andExpect(jsonPath("$.errors.message").value(SAME_USER.getMessage())) //error의 메시지
			.andDo(print());
	}

	@DisplayName("유저 신고 요청에 파라미터 값이 없으면 실패한다.")
	@Test
	void reportUserValidationTest() throws Exception {
		Error requiredUserIdError = Error.of(ValidExceptionCode.REQUIRED_USER_ID);
		Error reportTargetUserIdError = Error.of(ValidExceptionCode.REPORT_TARGET_USER_ID_REQUIRED);
		Error reportTypeError = Error.of(ValidExceptionCode.REPORT_TYPE_NOT_VALID);
		Error notBlankError = Error.of(ValidExceptionCode.NOT_BLANK);

		// given
		UserReportRequest request = new UserReportRequest(null, null, null, null);

		// then
		mockMvc.perform(post("/api/v1/reports/user")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content(mapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest()).andDo(print())
			.andExpect(jsonPath("$.errors[?(@.code == '" + requiredUserIdError.code() + "')].status")
				.value(requiredUserIdError.status().name()))
			.andExpect(jsonPath("$.errors[?(@.code == '" + requiredUserIdError.code() + "')].message")
				.value(requiredUserIdError.message()))
			.andExpect(jsonPath("$.errors[?(@.code == '" + reportTargetUserIdError.code() + "')].status")
				.value(reportTargetUserIdError.status().name()))
			.andExpect(jsonPath("$.errors[?(@.code == '" + reportTargetUserIdError.code() + "')].message")
				.value(reportTargetUserIdError.message()))
			.andExpect(jsonPath("$.errors[?(@.code == '" + reportTypeError.code() + "')].status")
				.value(reportTypeError.status().name()))
			.andExpect(jsonPath("$.errors[?(@.code == '" + reportTypeError.code() + "')].message")
				.value(reportTypeError.message()))
			.andExpect(jsonPath("$.errors[?(@.code == '" + notBlankError.code() + "')].status")
				.value(notBlankError.status().name()))
			.andExpect(jsonPath("$.errors[?(@.code == '" + notBlankError.code() + "')].message")
				.value(notBlankError.message()));
	}

	@DisplayName("유저 신고 요청에 파라미터 타입이 안 맞으면 실패한다.")
	@Test
	void reportUserValidationTypeTest() throws Exception {

		// given
		Map<String, Object> request = new HashMap<>();
		request.put("userId", "숫자가 아닌 어떤 값");
		request.put("reportUserId", "숫자가 아닌 어떤 값");
		request.put("type", 123);
		request.put("content", 123);

		// when & then
		mockMvc.perform(post("/api/v1/reports/user")
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf())
				.content(mapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andDo(print())
			.andExpect(jsonPath("$.errors[?(@.code == 'JSON_PASSING_FAILED')].message").value(hasItem(containsString("필드의 값이 잘못되었습니다."))))
			.andExpect(jsonPath("$.errors[?(@.code == 'JSON_PASSING_FAILED')].message").value(hasItem(containsString("해당 필드의 값의 타입을 확인해주세요."))));
	}

}
