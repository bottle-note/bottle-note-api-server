package app.docs.user

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.user.constant.SocialType
import app.bottlenote.user.constant.UserStatus
import app.bottlenote.user.constant.UserType
import app.bottlenote.user.dto.request.AdminUserSearchRequest
import app.bottlenote.user.dto.response.AdminUserListResponse
import app.bottlenote.user.presentation.AdminUsersController
import app.bottlenote.user.service.AdminUserService
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
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.request.RequestDocumentation.*
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import java.time.LocalDateTime

@WebMvcTest(
	controllers = [AdminUsersController::class],
	excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureRestDocs
@DisplayName("Admin Users 컨트롤러 RestDocs 테스트")
class AdminUsersControllerDocsTest {

	@Autowired
	private lateinit var mvc: MockMvcTester

	@MockitoBean
	private lateinit var adminUserService: AdminUserService

	@Test
	@DisplayName("유저 목록을 조회할 수 있다")
	fun listUsers() {
		// given
		val items = listOf(
			AdminUserListResponse(
				1L, "user1@example.com", "사용자1", "https://img.example.com/1.jpg",
				UserType.ROLE_USER, UserStatus.ACTIVE, listOf(SocialType.KAKAO),
				5L, 3L, 2L,
				LocalDateTime.of(2025, 1, 15, 10, 0), LocalDateTime.of(2026, 4, 1, 14, 30)
			),
			AdminUserListResponse(
				2L, "user2@example.com", "사용자2", null,
				UserType.ROLE_USER, UserStatus.ACTIVE, listOf(SocialType.GOOGLE, SocialType.APPLE),
				0L, 1L, 0L,
				LocalDateTime.of(2025, 6, 20, 9, 0), null
			)
		)
		val page = PageImpl(items)
		val response = GlobalResponse.fromPage(page)

		given(adminUserService.searchUsers(any(AdminUserSearchRequest::class.java)))
			.willReturn(response)

		// when & then
		assertThat(
			mvc.get().uri("/users?keyword=&status=ACTIVE&sortType=CREATED_AT&sortOrder=DESC&page=0&size=20")
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/users/list",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					queryParameters(
						parameterWithName("keyword").description("검색 키워드 (닉네임/이메일)").optional(),
						parameterWithName("status").description("유저 상태 필터 (ACTIVE/DELETED)").optional(),
						parameterWithName("sortType").description("정렬 기준 (CREATED_AT/NICK_NAME/EMAIL/RATING_COUNT/REVIEW_COUNT, 기본값: CREATED_AT)").optional(),
						parameterWithName("sortOrder").description("정렬 방향 (ASC/DESC, 기본값: DESC)").optional(),
						parameterWithName("page").description("페이지 번호 (0부터 시작, 기본값: 0)").optional(),
						parameterWithName("size").description("페이지 크기 (기본값: 20)").optional()
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
						fieldWithPath("data[]").type(JsonFieldType.ARRAY).description("유저 목록"),
						fieldWithPath("data[].userId").type(JsonFieldType.NUMBER).description("유저 ID"),
						fieldWithPath("data[].email").type(JsonFieldType.STRING).description("이메일"),
						fieldWithPath("data[].nickName").type(JsonFieldType.STRING).description("닉네임"),
						fieldWithPath("data[].imageUrl").type(JsonFieldType.VARIES).description("프로필 이미지 URL").optional(),
						fieldWithPath("data[].role").type(JsonFieldType.STRING).description("유저 권한 (ROLE_USER/ROLE_ADMIN)"),
						fieldWithPath("data[].status").type(JsonFieldType.STRING).description("유저 상태 (ACTIVE/DELETED)"),
						fieldWithPath("data[].socialType").type(JsonFieldType.ARRAY).description("소셜 로그인 타입 목록 (KAKAO/NAVER/GOOGLE/APPLE)"),
						fieldWithPath("data[].reviewCount").type(JsonFieldType.NUMBER).description("리뷰 수"),
						fieldWithPath("data[].ratingCount").type(JsonFieldType.NUMBER).description("별점 수"),
						fieldWithPath("data[].picksCount").type(JsonFieldType.NUMBER).description("찜 수"),
						fieldWithPath("data[].createAt").type(JsonFieldType.STRING).description("가입일"),
						fieldWithPath("data[].lastLoginAt").type(JsonFieldType.VARIES).description("최종 로그인일").optional(),
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
