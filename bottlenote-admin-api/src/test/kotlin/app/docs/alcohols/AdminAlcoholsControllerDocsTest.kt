package app.docs.alcohols

import app.bottlenote.alcohols.constant.AdminAlcoholSortType
import app.bottlenote.alcohols.constant.AlcoholCategoryGroup
import app.bottlenote.alcohols.dto.request.AdminAlcoholSearchRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholUpsertRequest
import app.bottlenote.alcohols.presentation.AdminAlcoholsController
import app.bottlenote.alcohols.service.AdminAlcoholCommandService
import app.bottlenote.alcohols.service.AlcoholQueryService
import app.bottlenote.global.dto.response.AdminResultResponse
import app.bottlenote.global.service.cursor.SortOrder
import app.helper.alcohols.AlcoholsHelper
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.apache.commons.lang3.tuple.Pair
import org.mockito.BDDMockito.given
import org.mockito.Mockito.any
import org.mockito.Mockito.anyLong
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.*
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

	@Autowired
	private lateinit var mapper: ObjectMapper

	@MockitoBean
	private lateinit var alcoholQueryService: AlcoholQueryService

	@MockitoBean
	private lateinit var adminAlcoholCommandService: AdminAlcoholCommandService

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
						fieldWithPath("data[].deletedAt").type(JsonFieldType.STRING).description("삭제일시").optional(),
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
	@DisplayName("관리자용 술 단건 상세 조회를 할 수 있다")
	fun getAlcoholDetail() {
		// given
		val response = AlcoholsHelper.createAdminAlcoholDetailResponse()

		given(alcoholQueryService.findAdminAlcoholDetailById(anyLong()))
			.willReturn(response)

		// when & then
		assertThat(
			mvc.get().uri("/alcohols/{alcoholId}", 1L)
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/alcohols/detail",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					pathParameters(
						parameterWithName("alcoholId").description("술 ID")
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("술 상세 정보"),
						fieldWithPath("data.alcoholId").type(JsonFieldType.NUMBER).description("술 ID"),
						fieldWithPath("data.korName").type(JsonFieldType.STRING).description("한글 이름"),
						fieldWithPath("data.engName").type(JsonFieldType.STRING).description("영문 이름"),
						fieldWithPath("data.imageUrl").type(JsonFieldType.STRING).description("이미지 URL"),
						fieldWithPath("data.type").type(JsonFieldType.STRING).description("술 타입 (WHISKY 등)"),
						fieldWithPath("data.korCategory").type(JsonFieldType.STRING).description("카테고리 한글명"),
						fieldWithPath("data.engCategory").type(JsonFieldType.STRING).description("카테고리 영문명"),
						fieldWithPath("data.categoryGroup").type(JsonFieldType.STRING).description("카테고리 그룹"),
						fieldWithPath("data.abv").type(JsonFieldType.STRING).description("도수"),
						fieldWithPath("data.age").type(JsonFieldType.STRING).description("숙성년도"),
						fieldWithPath("data.cask").type(JsonFieldType.STRING).description("캐스크 타입"),
						fieldWithPath("data.volume").type(JsonFieldType.STRING).description("용량"),
						fieldWithPath("data.description").type(JsonFieldType.STRING).description("설명"),
						fieldWithPath("data.regionId").type(JsonFieldType.NUMBER).description("지역 ID"),
						fieldWithPath("data.korRegion").type(JsonFieldType.STRING).description("지역 한글명"),
						fieldWithPath("data.engRegion").type(JsonFieldType.STRING).description("지역 영문명"),
						fieldWithPath("data.distilleryId").type(JsonFieldType.NUMBER).description("증류소 ID"),
						fieldWithPath("data.korDistillery").type(JsonFieldType.STRING).description("증류소 한글명"),
						fieldWithPath("data.engDistillery").type(JsonFieldType.STRING).description("증류소 영문명"),
						fieldWithPath("data.tastingTags").type(JsonFieldType.ARRAY).description("테이스팅 태그 목록"),
						fieldWithPath("data.tastingTags[].id").type(JsonFieldType.NUMBER).description("태그 ID"),
						fieldWithPath("data.tastingTags[].korName").type(JsonFieldType.STRING).description("태그 한글명"),
						fieldWithPath("data.tastingTags[].engName").type(JsonFieldType.STRING).description("태그 영문명"),
						fieldWithPath("data.avgRating").type(JsonFieldType.NUMBER).description("평균 평점"),
						fieldWithPath("data.totalRatingsCount").type(JsonFieldType.NUMBER).description("평점 수"),
						fieldWithPath("data.reviewCount").type(JsonFieldType.NUMBER).description("리뷰 수"),
						fieldWithPath("data.pickCount").type(JsonFieldType.NUMBER).description("찜 수"),
						fieldWithPath("data.createdAt").type(JsonFieldType.STRING).description("생성일시"),
						fieldWithPath("data.modifiedAt").type(JsonFieldType.STRING).description("수정일시"),
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

	@Test
	@DisplayName("관리자용 술을 생성할 수 있다")
	fun createAlcohol() {
		// given
		val response = AlcoholsHelper.createAdminResultResponse(
			code = AdminResultResponse.ResultCode.ALCOHOL_CREATED,
			targetId = 1L
		)
		val request = AlcoholsHelper.createAlcoholUpsertRequestMap()

		given(adminAlcoholCommandService.createAlcohol(any(AdminAlcoholUpsertRequest::class.java)))
			.willReturn(response)

		// when & then
		assertThat(
			mvc.post().uri("/alcohols")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/alcohols/create",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					requestFields(
						fieldWithPath("korName").type(JsonFieldType.STRING).description("한글 이름"),
						fieldWithPath("engName").type(JsonFieldType.STRING).description("영문 이름"),
						fieldWithPath("abv").type(JsonFieldType.STRING).description("도수"),
						fieldWithPath("type").type(JsonFieldType.STRING).description("술 타입 (WHISKY 등)"),
						fieldWithPath("korCategory").type(JsonFieldType.STRING).description("카테고리 한글명"),
						fieldWithPath("engCategory").type(JsonFieldType.STRING).description("카테고리 영문명"),
						fieldWithPath("categoryGroup").type(JsonFieldType.STRING).description("카테고리 그룹 (SINGLE_MALT 등)"),
						fieldWithPath("regionId").type(JsonFieldType.NUMBER).description("지역 ID"),
						fieldWithPath("distilleryId").type(JsonFieldType.NUMBER).description("증류소 ID"),
						fieldWithPath("age").type(JsonFieldType.STRING).description("숙성년도"),
						fieldWithPath("cask").type(JsonFieldType.STRING).description("캐스크 타입"),
						fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("이미지 URL"),
						fieldWithPath("description").type(JsonFieldType.STRING).description("설명"),
						fieldWithPath("volume").type(JsonFieldType.STRING).description("용량")
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 데이터"),
						fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드 (ALCOHOL_CREATED)"),
						fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
						fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("생성된 술 ID"),
						fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시간"),
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

	@Test
	@DisplayName("관리자용 술을 수정할 수 있다")
	fun updateAlcohol() {
		// given
		val response = AlcoholsHelper.createAdminResultResponse(
			code = AdminResultResponse.ResultCode.ALCOHOL_UPDATED,
			targetId = 1L
		)
		val request = AlcoholsHelper.createAlcoholUpsertRequestMap(
			korName = "수정된 위스키",
			engName = "Updated Whisky"
		)

		given(adminAlcoholCommandService.updateAlcohol(anyLong(), any(AdminAlcoholUpsertRequest::class.java)))
			.willReturn(response)

		// when & then
		assertThat(
			mvc.put().uri("/alcohols/{alcoholId}", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/alcohols/update",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					pathParameters(
						parameterWithName("alcoholId").description("수정할 술 ID")
					),
					requestFields(
						fieldWithPath("korName").type(JsonFieldType.STRING).description("한글 이름"),
						fieldWithPath("engName").type(JsonFieldType.STRING).description("영문 이름"),
						fieldWithPath("abv").type(JsonFieldType.STRING).description("도수"),
						fieldWithPath("type").type(JsonFieldType.STRING).description("술 타입 (WHISKY 등)"),
						fieldWithPath("korCategory").type(JsonFieldType.STRING).description("카테고리 한글명"),
						fieldWithPath("engCategory").type(JsonFieldType.STRING).description("카테고리 영문명"),
						fieldWithPath("categoryGroup").type(JsonFieldType.STRING).description("카테고리 그룹 (SINGLE_MALT 등)"),
						fieldWithPath("regionId").type(JsonFieldType.NUMBER).description("지역 ID"),
						fieldWithPath("distilleryId").type(JsonFieldType.NUMBER).description("증류소 ID"),
						fieldWithPath("age").type(JsonFieldType.STRING).description("숙성년도"),
						fieldWithPath("cask").type(JsonFieldType.STRING).description("캐스크 타입"),
						fieldWithPath("imageUrl").type(JsonFieldType.STRING).description("이미지 URL"),
						fieldWithPath("description").type(JsonFieldType.STRING).description("설명"),
						fieldWithPath("volume").type(JsonFieldType.STRING).description("용량")
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 데이터"),
						fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드 (ALCOHOL_UPDATED)"),
						fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
						fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("수정된 술 ID"),
						fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시간"),
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

	@Test
	@DisplayName("카테고리 레퍼런스를 조회할 수 있다")
	fun getCategoryReference() {
		// given
		val categoryPairs = listOf(
			Pair.of("싱글 몰트", "Single Malt"),
			Pair.of("블렌디드", "Blended"),
			Pair.of("버번", "Bourbon")
		)

		given(alcoholQueryService.findAllCategoryPairs())
			.willReturn(categoryPairs)

		// when & then
		assertThat(
			mvc.get().uri("/alcohols/categories/reference")
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/alcohols/category-reference",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
						fieldWithPath("data").type(JsonFieldType.ARRAY).description("카테고리 페어 목록"),
						fieldWithPath("data[].korCategory").type(JsonFieldType.STRING).description("한글 카테고리"),
						fieldWithPath("data[].engCategory").type(JsonFieldType.STRING).description("영문 카테고리"),
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

	@Test
	@DisplayName("관리자용 술을 삭제할 수 있다")
	fun deleteAlcohol() {
		// given
		val response = AlcoholsHelper.createAdminResultResponse(
			code = AdminResultResponse.ResultCode.ALCOHOL_DELETED,
			targetId = 1L
		)

		given(adminAlcoholCommandService.deleteAlcohol(anyLong()))
			.willReturn(response)

		// when & then
		assertThat(
			mvc.delete().uri("/alcohols/{alcoholId}", 1L)
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/alcohols/delete",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					pathParameters(
						parameterWithName("alcoholId").description("삭제할 술 ID")
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
						fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 데이터"),
						fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드 (ALCOHOL_DELETED)"),
						fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
						fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("삭제된 술 ID"),
						fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시간"),
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
