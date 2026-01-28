package app.docs.alcohols

import app.bottlenote.alcohols.dto.request.AdminReferenceSearchRequest
import app.bottlenote.alcohols.dto.request.AdminTastingTagUpsertRequest
import app.bottlenote.alcohols.dto.response.AdminAlcoholItem
import app.bottlenote.alcohols.dto.response.AdminTastingTagDetailResponse
import app.bottlenote.alcohols.dto.response.TastingTagNodeItem
import app.bottlenote.alcohols.presentation.AdminTastingTagController
import app.bottlenote.alcohols.service.AlcoholReferenceService
import app.bottlenote.alcohols.service.TastingTagService
import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.dto.response.AdminResultResponse
import app.helper.alcohols.AlcoholsHelper
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.*
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import java.time.LocalDateTime

@WebMvcTest(
	controllers = [AdminTastingTagController::class],
	excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureRestDocs
@DisplayName("Admin TastingTag 컨트롤러 RestDocs 테스트")
class AdminTastingTagControllerDocsTest {

	@Autowired
	private lateinit var mvc: MockMvcTester

	@Autowired
	private lateinit var mapper: ObjectMapper

	@MockitoBean
	private lateinit var alcoholReferenceService: AlcoholReferenceService

	@MockitoBean
	private lateinit var tastingTagService: TastingTagService

	@Nested
	@DisplayName("테이스팅 태그 목록 조회")
	inner class GetTastingTagList {

		@Test
		@DisplayName("테이스팅 태그 목록을 조회할 수 있다")
		fun getAllTastingTags() {
			// given
			val items = AlcoholsHelper.createTastingTagNodeItems(3)
			val page = PageImpl(items)
			val response = GlobalResponse.fromPage(page)

			given(alcoholReferenceService.findAllTastingTags(any(AdminReferenceSearchRequest::class.java)))
				.willReturn(response)

			// when & then
			assertThat(
				mvc.get().uri("/tasting-tags?keyword=&page=0&size=20&sortOrder=ASC")
			)
				.hasStatusOk()
				.apply(
					document(
						"admin/tasting-tags/list",
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
							fieldWithPath("data").type(JsonFieldType.ARRAY).description("테이스팅 태그 목록"),
							fieldWithPath("data[].id").type(JsonFieldType.NUMBER).description("태그 ID"),
							fieldWithPath("data[].korName").type(JsonFieldType.STRING).description("태그 한글명"),
							fieldWithPath("data[].engName").type(JsonFieldType.STRING).description("태그 영문명"),
							fieldWithPath("data[].icon").type(JsonFieldType.STRING).description("아이콘 (Base64)").optional(),
							fieldWithPath("data[].description").type(JsonFieldType.STRING).description("설명").optional(),
							fieldWithPath("data[].parent").type(JsonFieldType.OBJECT).description("부모 태그 (목록에서는 null)").optional(),
							fieldWithPath("data[].children").type(JsonFieldType.ARRAY).description("자식 태그 목록 (목록에서는 null)").optional(),
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

	@Nested
	@DisplayName("테이스팅 태그 상세 조회")
	inner class GetTastingTagDetail {

		@Test
		@DisplayName("테이스팅 태그 상세 정보를 조회할 수 있다")
		fun getTagDetail() {
			// given
			val childNode = TastingTagNodeItem.of(
				2L, "바닐라 크림", "Vanilla Cream", null, "바닐라 크림 향", null, emptyList()
			)
			val tagNode = TastingTagNodeItem.of(
				1L, "바닐라", "Vanilla", "base64icon", "바닐라 향", null, listOf(childNode)
			)
			val alcoholItem = AdminAlcoholItem(
				1L, "글렌피딕 12년", "Glenfiddich 12", "싱글몰트", "Single Malt",
				"https://example.com/image.jpg",
				LocalDateTime.of(2024, 1, 1, 0, 0), LocalDateTime.of(2024, 6, 1, 0, 0)
			)

			val response = AdminTastingTagDetailResponse.of(tagNode, listOf(alcoholItem))

			given(tastingTagService.getTagDetail(anyLong())).willReturn(response)

			// when & then
			assertThat(mvc.get().uri("/tasting-tags/{tagId}", 1L))
				.hasStatusOk()
				.apply(
					document(
						"admin/tasting-tags/detail",
						preprocessRequest(prettyPrint()),
						preprocessResponse(prettyPrint()),
						pathParameters(
							parameterWithName("tagId").description("태그 ID")
						),
						responseFields(
							fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
							fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
							fieldWithPath("data").type(JsonFieldType.OBJECT).description("태그 상세 정보"),
							fieldWithPath("data.tag").type(JsonFieldType.OBJECT).description("태그 트리 정보"),
							fieldWithPath("data.tag.id").type(JsonFieldType.NUMBER).description("태그 ID"),
							fieldWithPath("data.tag.korName").type(JsonFieldType.STRING).description("태그 한글명"),
							fieldWithPath("data.tag.engName").type(JsonFieldType.STRING).description("태그 영문명"),
							fieldWithPath("data.tag.icon").type(JsonFieldType.STRING).description("아이콘 (Base64)").optional(),
							fieldWithPath("data.tag.description").type(JsonFieldType.STRING).description("설명").optional(),
							fieldWithPath("data.tag.parent").type(JsonFieldType.OBJECT).description("부모 태그 (마트료시카 구조)").optional(),
							fieldWithPath("data.tag.children").type(JsonFieldType.ARRAY).description("자식 태그 목록 (마트료시카 구조)"),
							fieldWithPath("data.tag.children[].id").type(JsonFieldType.NUMBER).description("자식 태그 ID"),
							fieldWithPath("data.tag.children[].korName").type(JsonFieldType.STRING).description("자식 태그 한글명"),
							fieldWithPath("data.tag.children[].engName").type(JsonFieldType.STRING).description("자식 태그 영문명"),
							fieldWithPath("data.tag.children[].icon").type(JsonFieldType.STRING).description("자식 태그 아이콘").optional(),
							fieldWithPath("data.tag.children[].description").type(JsonFieldType.STRING).description("자식 태그 설명").optional(),
							fieldWithPath("data.tag.children[].parent").type(JsonFieldType.OBJECT).description("손자의 부모 (null)").optional(),
							fieldWithPath("data.tag.children[].children").type(JsonFieldType.ARRAY).description("손자 태그 목록"),
							fieldWithPath("data.alcohols").type(JsonFieldType.ARRAY).description("연결된 위스키 목록"),
							fieldWithPath("data.alcohols[].alcoholId").type(JsonFieldType.NUMBER).description("위스키 ID"),
							fieldWithPath("data.alcohols[].korName").type(JsonFieldType.STRING).description("위스키 한글명"),
							fieldWithPath("data.alcohols[].engName").type(JsonFieldType.STRING).description("위스키 영문명"),
							fieldWithPath("data.alcohols[].korCategoryName").type(JsonFieldType.STRING).description("카테고리 한글명"),
							fieldWithPath("data.alcohols[].engCategoryName").type(JsonFieldType.STRING).description("카테고리 영문명"),
							fieldWithPath("data.alcohols[].imageUrl").type(JsonFieldType.STRING).description("이미지 URL").optional(),
							fieldWithPath("data.alcohols[].createdAt").type(JsonFieldType.STRING).description("생성일시"),
							fieldWithPath("data.alcohols[].modifiedAt").type(JsonFieldType.STRING).description("수정일시"),
							fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
							fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
							fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
						)
					)
				)
		}
	}

	@Nested
	@DisplayName("테이스팅 태그 생성")
	inner class CreateTastingTag {

		@Test
		@DisplayName("테이스팅 태그를 생성할 수 있다")
		fun createTag() {
			// given
			val request = mapOf(
				"korName" to "바닐라",
				"engName" to "Vanilla",
				"icon" to AlcoholsHelper.VALID_BASE64_PNG,
				"description" to "바닐라 향",
				"parentId" to null
			)
			val response = AdminResultResponse.of(AdminResultResponse.ResultCode.TASTING_TAG_CREATED, 1L)

			given(tastingTagService.createTag(any(AdminTastingTagUpsertRequest::class.java)))
				.willReturn(response)

			// when & then
			assertThat(
				mvc.post().uri("/tasting-tags")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.apply(
					document(
						"admin/tasting-tags/create",
						preprocessRequest(prettyPrint()),
						preprocessResponse(prettyPrint()),
						requestFields(
							fieldWithPath("korName").type(JsonFieldType.STRING).description("태그 한글명 (필수)"),
							fieldWithPath("engName").type(JsonFieldType.STRING).description("태그 영문명 (필수)"),
							fieldWithPath("icon").type(JsonFieldType.STRING).description("아이콘 (Base64)").optional(),
							fieldWithPath("description").type(JsonFieldType.STRING).description("설명").optional(),
							fieldWithPath("parentId").type(JsonFieldType.NULL).description("부모 태그 ID (null이면 루트)").optional()
						),
						responseFields(
							fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
							fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
							fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
							fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
							fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
							fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("생성된 태그 ID"),
							fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시간"),
							fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
							fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
							fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
						)
					)
				)
		}
	}

	@Nested
	@DisplayName("테이스팅 태그 수정")
	inner class UpdateTastingTag {

		@Test
		@DisplayName("테이스팅 태그를 수정할 수 있다")
		fun updateTag() {
			// given
			val request = mapOf(
				"korName" to "바닐라 수정",
				"engName" to "Vanilla Updated",
				"icon" to AlcoholsHelper.VALID_BASE64_PNG,
				"description" to "수정된 설명"
			)
			val response = AdminResultResponse.of(AdminResultResponse.ResultCode.TASTING_TAG_UPDATED, 1L)

			given(tastingTagService.updateTag(anyLong(), any(AdminTastingTagUpsertRequest::class.java)))
				.willReturn(response)

			// when & then
			assertThat(
				mvc.put().uri("/tasting-tags/{tagId}", 1L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.apply(
					document(
						"admin/tasting-tags/update",
						preprocessRequest(prettyPrint()),
						preprocessResponse(prettyPrint()),
						pathParameters(
							parameterWithName("tagId").description("태그 ID")
						),
						requestFields(
							fieldWithPath("korName").type(JsonFieldType.STRING).description("태그 한글명 (필수)"),
							fieldWithPath("engName").type(JsonFieldType.STRING).description("태그 영문명 (필수)"),
							fieldWithPath("icon").type(JsonFieldType.STRING).description("아이콘 (Base64)").optional(),
							fieldWithPath("description").type(JsonFieldType.STRING).description("설명").optional(),
							fieldWithPath("parentId").type(JsonFieldType.NUMBER).description("부모 태그 ID").optional()
						),
						responseFields(
							fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
							fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
							fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
							fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
							fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
							fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("수정된 태그 ID"),
							fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시간"),
							fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
							fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
							fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
						)
					)
				)
		}
	}

	@Nested
	@DisplayName("테이스팅 태그 삭제")
	inner class DeleteTastingTag {

		@Test
		@DisplayName("테이스팅 태그를 삭제할 수 있다")
		fun deleteTag() {
			// given
			val response = AdminResultResponse.of(AdminResultResponse.ResultCode.TASTING_TAG_DELETED, 1L)

			given(tastingTagService.deleteTag(anyLong())).willReturn(response)

			// when & then
			assertThat(mvc.delete().uri("/tasting-tags/{tagId}", 1L))
				.hasStatusOk()
				.apply(
					document(
						"admin/tasting-tags/delete",
						preprocessRequest(prettyPrint()),
						preprocessResponse(prettyPrint()),
						pathParameters(
							parameterWithName("tagId").description("태그 ID")
						),
						responseFields(
							fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
							fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
							fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
							fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
							fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
							fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("삭제된 태그 ID"),
							fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시간"),
							fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
							fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
							fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
						)
					)
				)
		}
	}

	@Nested
	@DisplayName("테이스팅 태그 위스키 연결 관리")
	inner class ManageAlcohols {

		@Test
		@DisplayName("위스키를 태그에 벌크로 연결할 수 있다")
		fun addAlcoholsToTag() {
			// given
			val request = mapOf("alcoholIds" to listOf(1L, 2L, 3L))
			val response = AdminResultResponse.of(AdminResultResponse.ResultCode.TASTING_TAG_ALCOHOL_ADDED, 1L)

			given(tastingTagService.addAlcoholsToTag(anyLong(), any()))
				.willReturn(response)

			// when & then
			assertThat(
				mvc.post().uri("/tasting-tags/{tagId}/alcohols", 1L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.apply(
					document(
						"admin/tasting-tags/add-alcohols",
						preprocessRequest(prettyPrint()),
						preprocessResponse(prettyPrint()),
						pathParameters(
							parameterWithName("tagId").description("태그 ID")
						),
						requestFields(
							fieldWithPath("alcoholIds").type(JsonFieldType.ARRAY).description("연결할 위스키 ID 목록")
						),
						responseFields(
							fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
							fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
							fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
							fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
							fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
							fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("태그 ID"),
							fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시간"),
							fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
							fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
							fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
						)
					)
				)
		}

		@Test
		@DisplayName("위스키 연결을 벌크로 해제할 수 있다")
		fun removeAlcoholsFromTag() {
			// given
			val request = mapOf("alcoholIds" to listOf(1L, 2L))
			val response = AdminResultResponse.of(AdminResultResponse.ResultCode.TASTING_TAG_ALCOHOL_REMOVED, 1L)

			given(tastingTagService.removeAlcoholsFromTag(anyLong(), any()))
				.willReturn(response)

			// when & then
			assertThat(
				mvc.delete().uri("/tasting-tags/{tagId}/alcohols", 1L)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.apply(
					document(
						"admin/tasting-tags/remove-alcohols",
						preprocessRequest(prettyPrint()),
						preprocessResponse(prettyPrint()),
						pathParameters(
							parameterWithName("tagId").description("태그 ID")
						),
						requestFields(
							fieldWithPath("alcoholIds").type(JsonFieldType.ARRAY).description("해제할 위스키 ID 목록")
						),
						responseFields(
							fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
							fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
							fieldWithPath("data").type(JsonFieldType.OBJECT).description("결과 정보"),
							fieldWithPath("data.code").type(JsonFieldType.STRING).description("결과 코드"),
							fieldWithPath("data.message").type(JsonFieldType.STRING).description("결과 메시지"),
							fieldWithPath("data.targetId").type(JsonFieldType.NUMBER).description("태그 ID"),
							fieldWithPath("data.responseAt").type(JsonFieldType.STRING).description("응답 시간"),
							fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
							fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
							fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
						)
					)
				)
		}
	}
}
