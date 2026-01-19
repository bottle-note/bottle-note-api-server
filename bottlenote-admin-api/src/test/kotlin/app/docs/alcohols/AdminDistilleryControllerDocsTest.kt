package app.docs.alcohols

import app.bottlenote.alcohols.dto.request.AdminReferenceSearchRequest
import app.bottlenote.alcohols.presentation.AdminDistilleryController
import app.bottlenote.alcohols.service.AlcoholReferenceService
import app.bottlenote.global.data.response.GlobalResponse
import app.helper.alcohols.AlcoholsHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
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
	private lateinit var alcoholReferenceService: AlcoholReferenceService

	@Test
	@DisplayName("증류소 목록을 조회할 수 있다")
	fun getAllDistilleries() {
		// given
		val items = AlcoholsHelper.createAdminDistilleryItems(3)
		val page = PageImpl(items)
		val response = GlobalResponse.fromPage(page)

		given(alcoholReferenceService.findAllDistilleries(any(AdminReferenceSearchRequest::class.java)))
			.willReturn(response)

		// when & then
		assertThat(
			mvc.get().uri("/distilleries?keyword=&page=0&size=20&sortOrder=ASC")
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/distilleries/list",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					queryParameters(
						parameterWithName("keyword").description("검색어 (한글명/영문명)").optional(),
						parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
						parameterWithName("size").description("페이지 크기 (기본값: 20)").optional(),
						parameterWithName("sortOrder").description("정렬 방향 (ASC/DESC, 기본값: ASC)").optional()
					),
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
						fieldWithPath("meta.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
						fieldWithPath("meta.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
						fieldWithPath("meta.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
						fieldWithPath("meta.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
						fieldWithPath("meta.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
						fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).description("서버 버전").ignored(),
						fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).description("서버 인코딩").ignored(),
						fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).description("서버 응답 시간").ignored(),
						fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).description("API 경로 버전").ignored()
					)
				)
			)
	}
}
