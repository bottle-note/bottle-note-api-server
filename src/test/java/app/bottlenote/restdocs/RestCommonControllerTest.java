package app.bottlenote.restdocs;

import app.bottlenote.common.controller.CommonController;
import app.bottlenote.common.dto.request.RestdocsRequest;
import app.bottlenote.common.service.CommonService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//@WebMvcTest(RestCommonControllerTest.class)
@DisplayName("restdocs")
class RestCommonControllerTest extends AbstractRestDocs {
	private final CommonService commonService = mock(CommonService.class);
	@InjectMocks
	private CommonController controller;

	@Override
	protected Object initController() {
		return new CommonController(commonService);
	}

	@Test
	@DisplayName("restdocs example")
	void restdocs_example() throws Exception {
		// given
		LocalDateTime time = LocalDateTime.of(2021, 1, 1, 0, 0, 0);
		RestdocsRequest req = new RestdocsRequest("Test1", "테스트용 요청 DTO");

		// when
		when(commonService.restdocs()).thenReturn(time);

		// then
		mockMvc.perform(RestDocumentationRequestBuilders.get("/common/rest-docs/")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(req)))
			.andExpect(status().isOk())

			.andDo(
				document("common/rest-docs",
					requestFields(
						fieldWithPath("name").type(JsonFieldType.STRING).description("request parameter test name"),
						fieldWithPath("description").type(JsonFieldType.STRING).description("request parameter test description")
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("RestdocsResponse message"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("RestdocsResponse responseAt"),
						fieldWithPath("data.message").type(JsonFieldType.STRING).description("RestdocsResponse responseAt"),
						fieldWithPath("data.responseAt").type(JsonFieldType.ARRAY).description("RestdocsResponse responseAt"),
						fieldWithPath("errors").ignored(),
						fieldWithPath("meta.server_encoding").ignored(),
						fieldWithPath("meta.server_version").ignored(),
						fieldWithPath("meta.server_path_version").ignored(),
						fieldWithPath("meta.server_response_time").ignored()
					)
				)
			);
	}

}
