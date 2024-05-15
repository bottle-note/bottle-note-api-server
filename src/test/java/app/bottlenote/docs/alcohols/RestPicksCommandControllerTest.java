package app.bottlenote.docs.alcohols;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityUtil;
import app.bottlenote.picks.controller.PicksCommandController;
import app.bottlenote.picks.domain.PicksStatus;
import app.bottlenote.picks.dto.request.PicksUpdateRequest;
import app.bottlenote.picks.dto.response.PicksUpdateResponse;
import app.bottlenote.picks.service.PicksCommandService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Pick 컨트롤러 Rest API 문서화 테스트")
class RestPicksCommandControllerTest extends AbstractRestDocs {

	private final PicksCommandService picksCommandService = mock(PicksCommandService.class);

	@Override
	protected Object initController() {
		return new PicksCommandController(picksCommandService);
	}


	private MockedStatic<SecurityUtil> mockedSecurityUtil;

	@BeforeEach
	void setup() {
		mockedSecurityUtil = mockStatic(SecurityUtil.class);
		when(SecurityUtil.getCurrentUserId()).thenReturn(1L);
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@DisplayName("술을 찜할 수 있다.")
	@Test
	void updatePicks() throws Exception {
		// given
		PicksUpdateRequest request = new PicksUpdateRequest(1L, PicksStatus.PICK);
		// when & then
		PicksUpdateResponse response = PicksUpdateResponse.of(PicksStatus.PICK);

		when(picksCommandService.updatePicks(request, 1L)).thenReturn(response);

		mockMvc.perform(put("/apv/v1/picks")

				.content(objectMapper.writeValueAsString(request))
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(
				document("alcohols/picks",
					requestFields(
						fieldWithPath("alcoholId").description("찜 할 술(위스키)의 식별자"),
						fieldWithPath("isPicked").description("찜 여부 (하단 Picks Status 참조)")
					),
					responseFields(
						fieldWithPath("success").description("응답 성공 여부"),
						fieldWithPath("code").description("응답 코드(http status code)"),
						fieldWithPath("data.message").description("결과 메시지"),
						fieldWithPath("data.status").description("업데이트된 찜 상태"),
						fieldWithPath("errors").type(JsonFieldType.ARRAY).description("응답 성공 여부가 false일 경우 에러 메시지(없을 경우 null)"),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored()
					)
				)
			);
	}
}
