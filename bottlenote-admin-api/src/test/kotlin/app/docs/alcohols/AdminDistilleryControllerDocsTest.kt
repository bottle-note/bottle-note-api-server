package app.docs.alcohols

import app.bottlenote.alcohols.domain.DistilleryRepository
import app.bottlenote.alcohols.persentaton.AdminDistilleryController
import app.helper.alcohols.AlcoholsHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester

@WebMvcTest(
	controllers = [AdminDistilleryController::class],
	excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureRestDocs
@DisplayName("Admin Distillery 컨트롤러 RestDocs 테스트")
class AdminDistilleryControllerDocsTest {

	@Autowired
	private lateinit var mvc: MockMvcTester

	@MockitoBean
	private lateinit var distilleryRepository: DistilleryRepository

	@Test
	@DisplayName("증류소 목록을 조회할 수 있다")
	fun getAllDistilleries() {
		// given
		val items = AlcoholsHelper.createAdminDistilleryItems(3)

		given(distilleryRepository.findAllDistilleries())
			.willReturn(items)

		// when & then
		assertThat(
			mvc.get().uri("/distilleries")
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/distilleries/list",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
						fieldWithPath("data").type(JsonFieldType.ARRAY).description("증류소 목록"),
						fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("증류소 ID"),
						fieldWithPath("data[].korName").type(JsonFieldType.STRING).description("증류소 한글명"),
						fieldWithPath("data[].engName").type(JsonFieldType.STRING).description("증류소 영문명"),
						fieldWithPath("data[].logoImgUrl").type(JsonFieldType.STRING).description("로고 이미지 URL"),
						fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("생성일시"),
						fieldWithPath("data[].modifiedAt").type(JsonFieldType.STRING).description("수정일시"),
						fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
						fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
						fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).description("서버 버전").ignored(),
						fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).description("서버 인코딩").ignored(),
						fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).description("서버 응답 시간").ignored(),
						fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).description("API 경로 버전").ignored()
					)
				)
			)
	}
}
