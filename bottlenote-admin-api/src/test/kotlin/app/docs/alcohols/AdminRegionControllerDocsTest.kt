package app.docs.alcohols

import app.bottlenote.alcohols.dto.request.AdminReferenceSearchRequest
import app.bottlenote.alcohols.presentation.AdminRegionController
import app.bottlenote.alcohols.service.AdminRegionService
import app.bottlenote.alcohols.service.AlcoholReferenceService
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.dto.request.AdminBulkReorderRequest
import app.bottlenote.global.dto.response.AdminResultResponse
import app.helper.alcohols.AlcoholsHelper
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.pathParameters
import org.springframework.restdocs.request.RequestDocumentation.queryParameters
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester

@WebMvcTest(
	controllers = [AdminRegionController::class],
	excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureRestDocs
@Tag("restdocs")
@DisplayName("Admin Region 컨트롤러 RestDocs 테스트")
class AdminRegionControllerDocsTest {

	@Autowired
	private lateinit var mvc: MockMvcTester

	@Autowired
	private lateinit var mapper: ObjectMapper

	@MockitoBean
	private lateinit var alcoholReferenceService: AlcoholReferenceService

	@MockitoBean
	private lateinit var adminRegionService: AdminRegionService

	@Test
	@DisplayName("지역 목록을 조회할 수 있다")
	fun getAllRegions() {
		// given
		val items = AlcoholsHelper.createAdminRegionItems(3)
		val page = PageImpl(items)
		val response = GlobalResponse.fromPage(page)

		given(alcoholReferenceService.findAllRegionsForAdmin(any(AdminReferenceSearchRequest::class.java)))
			.willReturn(response)

		// when & then
		assertThat(
			mvc.get().uri("/v1/regions?keyword=&page=0&size=20&sortOrder=ASC")
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/regions/list",
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
						fieldWithPath("data").type(JsonFieldType.ARRAY).description("지역 목록"),
						fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("지역 ID"),
						fieldWithPath("data[].korName").type(JsonFieldType.STRING).description("국가 한글명"),
						fieldWithPath("data[].engName").type(JsonFieldType.STRING).description("국가 영문명"),
						fieldWithPath("data[].continent").type(JsonFieldType.STRING).description("대륙"),
						fieldWithPath("data[].description").type(JsonFieldType.STRING).description("설명"),
						fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("생성일시"),
						fieldWithPath("data[].modifiedAt").type(JsonFieldType.STRING).description("수정일시"),
						fieldWithPath("data[].parentId").type(JsonFieldType.NUMBER).description("상위 지역 ID").optional(),
						fieldWithPath("data[].sortOrder").type(JsonFieldType.NUMBER).description("정렬 순서 (미설정: 9999)"),
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

	@Test
	@DisplayName("전체 지역 목록을 bulk reorder 할 수 있다")
	fun reorderRegions() {
		val result = AdminResultResponse.of(AdminResultResponse.ResultCode.REGION_SORT_ORDER_UPDATED, null)
		given(adminRegionService.reorder(any(AdminBulkReorderRequest::class.java))).willReturn(result)
		val request = mapOf("ids" to listOf(3L, 1L, 6L, 5L))

		assertThat(
			mvc.patch().uri("/v1/regions/bulk/reorder")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
		).hasStatusOk()
			.apply(
				document(
					"admin/regions/bulk-reorder",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					requestFields(
						fieldWithPath("ids").type(JsonFieldType.ARRAY).description("맨 앞 구간으로 올릴 지역 ID 목록 (1~100개). 배열 순서가 최종 상대 순서입니다")
					),
					responseFields(bulkReorderResponseFields())
				)
			)
	}

	@Test
	@DisplayName("자식 지역 목록을 bulk reorder 할 수 있다")
	fun reorderChildRegions() {
		val result = AdminResultResponse.of(AdminResultResponse.ResultCode.REGION_SORT_ORDER_UPDATED, 1L)
		given(adminRegionService.reorderChildren(anyLong(), any(AdminBulkReorderRequest::class.java))).willReturn(result)
		val request = mapOf("ids" to listOf(30L, 10L, 60L, 50L))

		assertThat(
			mvc.patch().uri("/v1/regions/{parentId}/children/bulk/reorder", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
		).hasStatusOk()
			.apply(
				document(
					"admin/regions/children-bulk-reorder",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					pathParameters(parameterWithName("parentId").description("부모 지역 ID")),
					requestFields(
						fieldWithPath("ids").type(JsonFieldType.ARRAY).description("맨 앞 구간으로 올릴 직접 자식 지역 ID 목록 (1~100개). 배열 순서가 최종 상대 순서입니다")
					),
					responseFields(bulkReorderResponseFields())
				)
			)
	}

	private fun bulkReorderResponseFields() = listOf(
		fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
		fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
		fieldWithPath("data.code").type(JsonFieldType.STRING).description("처리 결과 코드"),
		fieldWithPath("data.message").type(JsonFieldType.STRING).description("처리 결과 메시지"),
		fieldWithPath("data.targetId").type(JsonFieldType.VARIES).description("대상 ID. 전체 bulk reorder는 null, 자식 지역 bulk reorder는 parentId"),
		fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시각"),
		fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
		fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보").ignored(),
		fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).description("서버 버전").ignored(),
		fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).description("서버 인코딩").ignored(),
		fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).description("서버 응답 시간").ignored(),
		fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).description("API 경로 버전").ignored()
	)
}
