package app.docs.alcohols

import app.bottlenote.alcohols.constant.AdminAlcoholSortType
import app.bottlenote.alcohols.constant.AlcoholCategoryGroup
import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest
import app.bottlenote.alcohols.persentaton.AdminAlcoholsController
import app.bottlenote.alcohols.service.AlcoholQueryService
import app.bottlenote.global.service.cursor.SortOrder
import app.helper.alcohols.AlcoholsHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
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
	controllers = [AdminAlcoholsController::class],
	excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureRestDocs
@DisplayName("Admin Alcohol 컨트롤러 RestDocs 테스트")
class AdminAlcoholsControllerDocsTest {

	@Autowired
	private lateinit var mvc: MockMvcTester

	@MockitoBean
	private lateinit var alcoholQueryService: AlcoholQueryService

	@Test
	@DisplayName("관리자용 술 목록을 조회할 수 있다")
	fun searchAdminAlcohols() {
		// given
		val items = AlcoholsHelper.createAdminAlcoholItems(2)
		val response = AlcoholsHelper.createPageResponse(items)

		given(alcoholQueryService.searchAdminAlcohols(any(AdminAlcoholSearchRequest::class.java)))
			.willReturn(response)

		// when & then
		assertThat(
			mvc.get().uri("/alcohols")
				.param("keyword", "글렌")
				.param("category", AlcoholCategoryGroup.SINGLE_MALT.name)
				.param("regionId", "1")
				.param("sortType", AdminAlcoholSortType.KOR_NAME.name)
				.param("sortOrder", SortOrder.ASC.name)
				.param("page", "0")
				.param("size", "20")
		)
			.hasStatusOk()
			.bodyJson()
			.extractingPath("$.success").isEqualTo(true)

		assertThat(
			mvc.get().uri("/alcohols")
				.param("keyword", "글렌")
				.param("category", AlcoholCategoryGroup.SINGLE_MALT.name)
				.param("regionId", "1")
				.param("sortType", AdminAlcoholSortType.KOR_NAME.name)
				.param("sortOrder", SortOrder.ASC.name)
				.param("page", "0")
				.param("size", "20")
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/alcohols/search",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					queryParameters(
						parameterWithName("keyword").optional().description("검색어 (한글/영문 이름 검색)"),
						parameterWithName("category").optional().description("카테고리 그룹 필터 (예: SINGLE_MALT, BLEND 등)"),
						parameterWithName("regionId").optional().description("지역 ID 필터"),
						parameterWithName("sortType").optional().description("정렬 기준 (KOR_NAME, ENG_NAME, KOR_CATEGORY, ENG_CATEGORY / 기본값: KOR_NAME)"),
						parameterWithName("sortOrder").optional().description("정렬 방향 (기본값: ASC)"),
						parameterWithName("page").optional().description("페이지 번호 (기본값: 0)"),
						parameterWithName("size").optional().description("페이지 크기 (기본값: 20)")
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
						fieldWithPath("data").type(JsonFieldType.ARRAY).description("술 목록 데이터"),
						fieldWithPath("data[].alcoholId").type(JsonFieldType.NUMBER).description("술 ID"),
						fieldWithPath("data[].korName").type(JsonFieldType.STRING).description("술 한글 이름"),
						fieldWithPath("data[].engName").type(JsonFieldType.STRING).description("술 영문 이름"),
						fieldWithPath("data[].korCategoryName").type(JsonFieldType.STRING).description("카테고리 한글명"),
						fieldWithPath("data[].engCategoryName").type(JsonFieldType.STRING).description("카테고리 영문명"),
						fieldWithPath("data[].imageUrl").type(JsonFieldType.STRING).description("술 이미지 URL"),
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
