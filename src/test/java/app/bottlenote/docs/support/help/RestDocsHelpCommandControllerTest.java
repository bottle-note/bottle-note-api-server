package app.bottlenote.docs.support.help;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.support.help.controller.HelpCommandController;
import app.bottlenote.support.help.dto.request.HelpRegisterRequest;
import app.bottlenote.support.help.dto.response.HelpRegisterResponse;
import app.bottlenote.support.help.fixture.HelpObjectFixture;
import app.bottlenote.support.help.service.HelpService;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

@DisplayName("문의글 커맨드 컨트롤러 RestDocs용 테스트")
class RestDocsHelpCommandControllerTest extends AbstractRestDocs {

	private final HelpService helpService = mock(HelpService.class);

	private final MockedStatic<SecurityContextUtil> mockedSecurityUtil = mockStatic(SecurityContextUtil.class);

	private final HelpRegisterRequest helpRegisterRequest = HelpObjectFixture.getHelpRegisterRequest();
	private final HelpRegisterResponse successResponse = HelpObjectFixture.getSuccessHelpRegisterResponse();


	@Override
	protected Object initController() {
		return new HelpCommandController(helpService);
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@Test
	@DisplayName("문의글을 작성할 수 있다.")
	void review_delete_test() throws Exception {

		Long userId = 1L;

		//when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

		when(helpService.registerHelp(any(HelpRegisterRequest.class), anyLong()))
			.thenReturn(successResponse);

		//then
		mockMvc.perform(post("/api/v1/help")
				.content(objectMapper.writeValueAsString(helpRegisterRequest))
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(
				document("support/help/help-register",
					requestFields(
						fieldWithPath("title").type(JsonFieldType.STRING).description("문의글 제목"),
						fieldWithPath("content").type(JsonFieldType.STRING).description("문의글 내용"),
						fieldWithPath("type").type(JsonFieldType.STRING).description("문의글 타입  (WHISKEY, REVIEW, USER, ETC)")
					),
					responseFields(
						fieldWithPath("success").description("응답 성공 여부"),
						fieldWithPath("code").description("응답 코드(http status code)"),
						fieldWithPath("data.codeMessage").description("성공 메시지 코드"),
						fieldWithPath("data.message").description("성공 메시지"),
						fieldWithPath("data.helpId").description("문의글 아이디"),
						fieldWithPath("data.responseAt").description("서버 응답 일시"),
						fieldWithPath("errors").ignored(),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored()
					)
				)
			);
	}
}
