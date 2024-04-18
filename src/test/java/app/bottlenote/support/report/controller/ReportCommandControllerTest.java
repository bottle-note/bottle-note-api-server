package app.bottlenote.support.report.controller;

import app.bottlenote.support.report.domain.constant.UserReportType;
import app.bottlenote.support.report.dto.request.UserReportRequest;
import app.bottlenote.support.report.dto.response.UserReportResponse;
import app.bottlenote.support.report.service.UserReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static app.bottlenote.support.report.dto.response.UserReportResponse.UserReportResponseEnum.SAME_USER;
import static app.bottlenote.support.report.dto.response.UserReportResponse.UserReportResponseEnum.SUCCESS;
import static app.bottlenote.support.report.dto.response.UserReportResponse.of;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportCommandController.class)
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
		// given
		UserReportRequest request = new UserReportRequest(null, null, null, null);
		// then
		mockMvc.perform(post("/api/v1/reports/user")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value("false"))
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.data").isEmpty())
			.andExpect(jsonPath("$.errors.userId").value("신고자 아이디는 필수입니다."))
			.andExpect(jsonPath("$.errors.reportUserId").value("신고 대상자 아이디는 필수입니다."))
			.andExpect(jsonPath("$.errors.type").value("신고 타입이 적절하지 않습니다. ( SPAM , INAPPROPRIATE_CONTENT ,FRAUD ,COPYRIGHT_INFRINGEMENT ,OTHER )"))
			.andExpect(jsonPath("$.errors.content").value("신고 내용은 필수입니다."))
			.andDo(print());
	}

	@DisplayName("유저 신고 요청에 파라미터 타입이 안맞으면 실패한다.")
	@Test
	void reportUserValidationTypeTest() throws Exception {
		// given
		Map<String, Object> request = new HashMap<>();
		request.put("userId", "숫자가 아닌 어떤 값");
		request.put("reportUserId", "숫자가 아닌 어떤 값");
		request.put("type", 123);
		request.put("content", 123);

		// then
		mockMvc.perform(post("/api/v1/reports/user")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value("false"))
			.andExpect(jsonPath("$.code").value("400"))
			.andExpect(jsonPath("$.data").isEmpty())
			.andExpect(jsonPath("$.errors.message", containsString("필드의 값이 잘못되었습니다.")))
			.andExpect(jsonPath("$.errors.message", containsString("해당 필드의 값의 타입을 확인해주세요.")))
			.andDo(print());
	}
}
