package app.bottlenote.docs.support.report;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.support.report.constant.UserReportType;
import app.bottlenote.support.report.controller.ReportCommandController;
import app.bottlenote.support.report.dto.request.UserReportRequest;
import app.bottlenote.support.report.dto.response.UserReportResponse;
import app.bottlenote.support.report.service.ReviewReportService;
import app.bottlenote.support.report.service.UserReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.Optional;

import static app.bottlenote.support.report.dto.response.UserReportResponse.UserReportResponseEnum.SUCCESS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("유저 신고 커맨드 컨트롤러 RestDocs용 테스트")
class RestDocsReportCommandControllerTest extends AbstractRestDocs {

	private final UserReportService userReportService = mock(UserReportService.class);
	private final ReviewReportService reviewReportService = mock(ReviewReportService.class);
	private final MockedStatic<SecurityContextUtil> mockedSecurityUtil = mockStatic(SecurityContextUtil.class);

	@Override
	protected Object initController() {
		return new ReportCommandController(userReportService, reviewReportService);
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@DisplayName("[restdocs] 유저를 신고 할 수 있다.")
	@Test
	void reportUserTest() throws Exception {
		// given
		final Long currentUserId = 1L;
		final UserReportRequest request = new UserReportRequest(2L, UserReportType.OTHER, "신고 내용 쏼라 쏼라 쏼라 ");
		final UserReportResponse response = UserReportResponse.of(SUCCESS, 1L, 2L, "신고한 유저 이름");

		// when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(currentUserId));
		when(userReportService.userReport(currentUserId, request)).thenReturn(response);

		// then
		mockMvc.perform(post("/api/v1/reports/user")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())

			.andDo(
				document("support/report/user-report",
					requestFields(
						fieldWithPath("reportUserId").type(JsonFieldType.NUMBER).description("신고 대상자 유저 ID(신고를 당하는 유저"),
						fieldWithPath("type").type(JsonFieldType.STRING).description("신고 타입  ( SPAM , INAPPROPRIATE_CONTENT ,FRAUD ,COPYRIGHT_INFRINGEMENT ,OTHER )"),
						fieldWithPath("content").type(JsonFieldType.STRING).description("신고 내용")
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드(http status code)"),
						fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
						fieldWithPath("data.reportId").type(JsonFieldType.NUMBER).description("처리된 신고 ID"),
						fieldWithPath("data.reportUserId").type(JsonFieldType.NUMBER).description("신고 대상 유저 ID"),
						fieldWithPath("data.reportUserName").type(JsonFieldType.STRING).description("신고 대상 유저 이름"),
						fieldWithPath("errors").type(JsonFieldType.ARRAY).description("응답 성공 여부가 false일 경우 에러 메시지(없을 경우 null)"),
						fieldWithPath("meta.serverEncoding").description("서버 인코딩 정도"),
						fieldWithPath("meta.serverVersion").description("서버 버전"),
						fieldWithPath("meta.serverPathVersion").description("서버 경로 버전"),
						fieldWithPath("meta.serverResponseTime").description("서버 응답 시간")
					)
				)
			);
	}
}
