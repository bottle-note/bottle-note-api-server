package app

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(
	controllers = [HelloAdminApiController::class],
	excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureRestDocs
class HelloAdminApiControllerTest {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Test
	@DisplayName("Admin API Hello 엔드포인트를 호출할 수 있다")
	fun helloTest() {
		mockMvc.perform(get("/admin/api/v1/hello"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.message").value("Hello World!"))
			.andDo(
				document(
					"admin-hello",
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
						fieldWithPath("data.message").type(JsonFieldType.STRING).description("메시지"),
						fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
						fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
						fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).description("서버 버전"),
						fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).description("인코딩"),
						fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).description("응답 시간"),
						fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).description("경로 버전")
					)
				)
			)
	}
}
