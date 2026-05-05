package app.docs.alcohols

import app.bottlenote.alcohols.dto.request.AdminDistilleryUpsertRequest
import app.bottlenote.alcohols.dto.request.AdminReferenceSearchRequest
import app.bottlenote.alcohols.dto.response.AdminDistilleryItem
import app.bottlenote.alcohols.presentation.AdminDistilleryController
import app.bottlenote.alcohols.service.AlcoholReferenceService
import app.bottlenote.alcohols.service.DistilleryService
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.dto.response.AdminResultResponse
import app.bottlenote.global.dto.response.AdminResultResponse.ResultCode
import app.helper.alcohols.AlcoholsHelper
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
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
import java.time.LocalDateTime

@WebMvcTest(
	controllers = [AdminDistilleryController::class],
	excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureRestDocs
@DisplayName("Admin Distillery 컨트롤러 RestDocs 테스트")
class AdminDistilleryControllerDocsTest {

	@Autowired
	private lateinit var mvc: MockMvcTester

	@Autowired
	private lateinit var mapper: ObjectMapper

	@MockitoBean
	private lateinit var alcoholReferenceService: AlcoholReferenceService

	@MockitoBean
	private lateinit var distilleryService: DistilleryService

	@Test
	@DisplayName("증류소 목록을 조회할 수 있다")
	fun getAllDistilleries() {
		val items = AlcoholsHelper.createAdminDistilleryItems(3)
		val page = PageImpl(items)
		val response = GlobalResponse.fromPage(page)

		given(alcoholReferenceService.findAllDistilleries(any(AdminReferenceSearchRequest::class.java)))
			.willReturn(response)

		assertThat(
			mvc.get().uri("/distilleries?keyword=&page=0&size=20&sortOrder=ASC")
		).hasStatusOk()
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
						fieldWithPath("data[].imageUrl").type(JsonFieldType.STRING).description("증류소 이미지 URL"),
						fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("생성일시"),
						fieldWithPath("data[].modifiedAt").type(JsonFieldType.STRING).description("수정일시"),
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
	@DisplayName("증류소 상세를 조회할 수 있다")
	fun getDistilleryDetail() {
		val item = AdminDistilleryItem(
			1L,
			"맥캘란",
			"Macallan",
			"https://example.com/logo.png",
			LocalDateTime.now(),
			LocalDateTime.now()
		)
		given(distilleryService.getDetail(anyLong())).willReturn(item)

		assertThat(
			mvc.get().uri("/distilleries/{distilleryId}", 1L)
		).hasStatusOk()
			.apply(
				document(
					"admin/distilleries/detail",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					pathParameters(parameterWithName("distilleryId").description("증류소 ID")),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
						fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("증류소 ID"),
						fieldWithPath("data.korName").type(JsonFieldType.STRING).description("증류소 한글명"),
						fieldWithPath("data.engName").type(JsonFieldType.STRING).description("증류소 영문명"),
						fieldWithPath("data.imageUrl").type(JsonFieldType.STRING).description("증류소 이미지 URL").optional(),
						fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성일시"),
						fieldWithPath("data.modifiedAt").type(JsonFieldType.STRING).description("수정일시"),
						fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
						fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보").ignored(),
						fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).description("서버 버전").ignored(),
						fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).description("서버 인코딩").ignored(),
						fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).description("서버 응답 시간").ignored(),
						fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).description("API 경로 버전").ignored()
					)
				)
			)
	}

	@Test
	@DisplayName("증류소를 생성할 수 있다")
	fun createDistillery() {
		val result = AdminResultResponse.of(ResultCode.DISTILLERY_CREATED, 1L)
		given(distilleryService.create(any(AdminDistilleryUpsertRequest::class.java))).willReturn(result)
		val request = mapOf(
			"korName" to "토버모리",
			"engName" to "Tobermory",
			"imageUrl" to "https://cdn.example.com/distillery/macallan.jpg"
		)

		assertThat(
			mvc.post().uri("/distilleries")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
		).hasStatusOk()
			.apply(
				document(
					"admin/distilleries/create",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					requestFields(
						fieldWithPath("korName").type(JsonFieldType.STRING).description("증류소 한글명 (필수)"),
						fieldWithPath("engName").type(JsonFieldType.STRING).description("증류소 영문명 (필수)"),
						fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("증류소 이미지 URL (S3 업로드 후 받은 URL)").optional()
					),
					responseFields(cudResponseFields())
				)
			)
	}

	@Test
	@DisplayName("증류소를 수정할 수 있다")
	fun updateDistillery() {
		val result = AdminResultResponse.of(ResultCode.DISTILLERY_UPDATED, 1L)
		given(distilleryService.update(anyLong(), any(AdminDistilleryUpsertRequest::class.java)))
			.willReturn(result)
		val request = mapOf(
			"korName" to "맥캘란 12년",
			"engName" to "Macallan 12",
			"imageUrl" to "https://cdn.example.com/distillery/macallan.jpg"
		)

		assertThat(
			mvc.put().uri("/distilleries/{distilleryId}", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
		).hasStatusOk()
			.apply(
				document(
					"admin/distilleries/update",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					pathParameters(parameterWithName("distilleryId").description("증류소 ID")),
					requestFields(
						fieldWithPath("korName").type(JsonFieldType.STRING).description("증류소 한글명 (필수)"),
						fieldWithPath("engName").type(JsonFieldType.STRING).description("증류소 영문명 (필수)"),
						fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("증류소 이미지 URL (S3 업로드 후 받은 URL)").optional()
					),
					responseFields(cudResponseFields())
				)
			)
	}

	@Test
	@DisplayName("증류소를 삭제할 수 있다")
	fun deleteDistillery() {
		val result = AdminResultResponse.of(ResultCode.DISTILLERY_DELETED, 1L)
		given(distilleryService.delete(anyLong())).willReturn(result)

		assertThat(
			mvc.delete().uri("/distilleries/{distilleryId}", 1L)
		).hasStatusOk()
			.apply(
				document(
					"admin/distilleries/delete",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					pathParameters(parameterWithName("distilleryId").description("증류소 ID")),
					responseFields(cudResponseFields())
				)
			)
	}

	private fun cudResponseFields() = listOf(
		fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
		fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
		fieldWithPath("data.code").type(JsonFieldType.STRING).description("처리 결과 코드"),
		fieldWithPath("data.message").type(JsonFieldType.STRING).description("처리 결과 메시지"),
		fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("대상 증류소 ID"),
		fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시각"),
		fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
		fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보").ignored(),
		fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).description("서버 버전").ignored(),
		fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).description("서버 인코딩").ignored(),
		fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).description("서버 응답 시간").ignored(),
		fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).description("API 경로 버전").ignored()
	)
}
