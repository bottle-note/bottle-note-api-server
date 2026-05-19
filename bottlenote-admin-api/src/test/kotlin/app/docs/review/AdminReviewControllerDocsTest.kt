package app.docs.review

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.service.cursor.SortOrder
import app.bottlenote.review.constant.AdminReviewSortType
import app.bottlenote.review.constant.ReviewActiveStatus
import app.bottlenote.review.constant.ReviewDisplayStatus
import app.bottlenote.review.dto.request.AdminReviewSearchRequest
import app.bottlenote.review.dto.response.AdminReviewListResponse
import app.bottlenote.review.presentation.AdminReviewController
import app.bottlenote.review.service.AdminReviewQueryService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
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
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.*
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import java.time.LocalDateTime

@WebMvcTest(
	controllers = [AdminReviewController::class],
	excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureRestDocs
@DisplayName("Admin Review 컨트롤러 RestDocs 테스트")
@Tag("restdocs")
class AdminReviewControllerDocsTest {

	@Autowired
	private lateinit var mvc: MockMvcTester

	@MockitoBean
	private lateinit var adminReviewQueryService: AdminReviewQueryService

	@Test
	@DisplayName("관리자용 리뷰 목록을 조회할 수 있다")
	fun listReviews() {
		// given
		val items = listOf(
			AdminReviewListResponse(
				1L,
				101L,
				"글렌피딕 12년",
				1001L,
				"reviewer1",
				"향이 풍부하고 균형감이 좋습니다.",
				4.5,
				ReviewActiveStatus.ACTIVE,
				ReviewDisplayStatus.PUBLIC,
				3L,
				LocalDateTime.of(2026, 1, 10, 10, 30),
				LocalDateTime.of(2026, 1, 12, 9, 15)
			),
			AdminReviewListResponse(
				2L,
				102L,
				"맥켈란 12년",
				1002L,
				"reviewer2",
				"비공개 처리된 운영 확인 대상 리뷰입니다.",
				3.0,
				ReviewActiveStatus.DISABLED,
				ReviewDisplayStatus.PRIVATE,
				0L,
				LocalDateTime.of(2026, 1, 9, 18, 0),
				LocalDateTime.of(2026, 1, 9, 18, 0)
			)
		)
		val response = GlobalResponse.fromPage(PageImpl(items))

		given(adminReviewQueryService.searchReviews(any(AdminReviewSearchRequest::class.java)))
			.willReturn(response)

		// when & then
		assertThat(
			mvc.get().uri("/v1/reviews")
				.param("alcoholId", "101")
				.param("userId", "1001")
				.param("activeStatus", ReviewActiveStatus.ACTIVE.name)
				.param("displayStatus", ReviewDisplayStatus.PUBLIC.name)
				.param("keyword", "글렌")
				.param("createdFrom", "2026-01-01T00:00:00")
				.param("createdTo", "2026-01-31T23:59:59")
				.param("sortType", AdminReviewSortType.CREATED_AT.name)
				.param("sortOrder", SortOrder.DESC.name)
				.param("page", "0")
				.param("size", "20")
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/reviews/list",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					queryParameters(
						parameterWithName("alcoholId").description("술 ID 필터").optional(),
						parameterWithName("userId").description("작성자 유저 ID 필터").optional(),
						parameterWithName("activeStatus").description("리뷰 활성 상태 필터 (ACTIVE/DELETED/DISABLED)").optional(),
						parameterWithName("displayStatus").description("리뷰 노출 상태 필터 (PUBLIC/PRIVATE)").optional(),
						parameterWithName("keyword").description("검색어 (리뷰 본문/작성자 닉네임/작성자 이메일/술 한글명/술 영문명)").optional(),
						parameterWithName("createdFrom").description("작성일시 시작 범위 (ISO-8601 LocalDateTime)").optional(),
						parameterWithName("createdTo").description("작성일시 종료 범위 (ISO-8601 LocalDateTime)").optional(),
						parameterWithName("sortType").description("정렬 기준 (기본값: CREATED_AT, 상세 값은 AdminReviewSortType 표 참조)").optional(),
						parameterWithName("sortOrder").description("정렬 방향 (기본값: DESC, 상세 값은 SortOrder 표 참조)").optional(),
						parameterWithName("page").description("페이지 번호 (0 이상, 기본값: 0)").optional(),
						parameterWithName("size").description("페이지 크기 (1~100, 기본값: 20)").optional()
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
						fieldWithPath("data[]").type(JsonFieldType.ARRAY).description("리뷰 목록"),
						fieldWithPath("data[].reviewId").type(JsonFieldType.NUMBER).description("리뷰 ID"),
						fieldWithPath("data[].alcoholId").type(JsonFieldType.NUMBER).description("술 ID"),
						fieldWithPath("data[].alcoholName").type(JsonFieldType.STRING).description("술 이름"),
						fieldWithPath("data[].userId").type(JsonFieldType.NUMBER).description("작성자 유저 ID"),
						fieldWithPath("data[].userNickname").type(JsonFieldType.STRING).description("작성자 닉네임"),
						fieldWithPath("data[].content").type(JsonFieldType.STRING).description("리뷰 본문"),
						fieldWithPath("data[].reviewRating").type(JsonFieldType.NUMBER).description("리뷰 평점"),
						fieldWithPath("data[].activeStatus").type(JsonFieldType.STRING).description("리뷰 활성 상태 (ACTIVE/DELETED/DISABLED)"),
						fieldWithPath("data[].displayStatus").type(JsonFieldType.STRING).description("리뷰 노출 상태 (PUBLIC/PRIVATE)"),
						fieldWithPath("data[].replyCount").type(JsonFieldType.NUMBER).description("댓글 수"),
						fieldWithPath("data[].createAt").type(JsonFieldType.STRING).description("리뷰 작성일시"),
						fieldWithPath("data[].lastModifyAt").type(JsonFieldType.STRING).description("리뷰 최종 수정일시"),
						fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
						fieldWithPath("meta").type(JsonFieldType.OBJECT).description("메타 정보"),
						fieldWithPath("meta.page").type(JsonFieldType.NUMBER).description("현재 페이지 번호"),
						fieldWithPath("meta.size").type(JsonFieldType.NUMBER).description("페이지 크기"),
						fieldWithPath("meta.totalElements").type(JsonFieldType.NUMBER).description("전체 요소 수"),
						fieldWithPath("meta.totalPages").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
						fieldWithPath("meta.hasNext").type(JsonFieldType.BOOLEAN).description("다음 페이지 존재 여부"),
						fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
						fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
						fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
						fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
					)
				)
			)
	}
}
