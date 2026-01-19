package app.docs.auth

import app.bottlenote.auth.config.RootAdminProperties
import app.bottlenote.auth.presentation.AuthController
import app.bottlenote.global.security.SecurityContextUtil
import app.bottlenote.user.constant.AdminRole
import app.bottlenote.user.dto.request.AdminSignupRequest
import app.bottlenote.user.dto.response.AdminSignupResponse
import app.bottlenote.user.service.AdminAuthService
import app.helper.auth.AuthHelper
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.BDDMockito.given
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.operation.preprocess.Preprocessors.*
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.assertj.MockMvcTester
import java.util.*

@WebMvcTest(
	controllers = [AuthController::class],
	excludeAutoConfiguration = [SecurityAutoConfiguration::class]
)
@AutoConfigureRestDocs
@DisplayName("Admin Auth 컨트롤러 RestDocs 테스트")
class AuthControllerDocsTest {

	@Autowired
	private lateinit var mvc: MockMvcTester

	@Autowired
	private lateinit var objectMapper: ObjectMapper

	@MockitoBean
	private lateinit var authService: AdminAuthService

	@MockitoBean
	private lateinit var rootAdminProperties: RootAdminProperties

	@MockitoBean
	private lateinit var passwordEncoder: BCryptPasswordEncoder

	@Test
	@DisplayName("어드민 로그인")
	fun login() {
		// given
		val tokenItem = AuthHelper.createTokenItem()
		given(authService.login(anyString(), anyString())).willReturn(tokenItem)

		val request = mapOf("email" to "admin@bottlenote.com", "password" to "password123")

		// when & then
		assertThat(
			mvc.post().uri("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/auth/login",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					requestFields(
						fieldWithPath("email").type(JsonFieldType.STRING).description("관리자 이메일"),
						fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호")
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
						fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("액세스 토큰"),
						fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰"),
						fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
						fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
						fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
						fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
						fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
					)
				)
			)
	}

	@Test
	@DisplayName("토큰 갱신")
	fun refresh() {
		// given
		val tokenItem = AuthHelper.createTokenItem()
		given(authService.refresh(anyString())).willReturn(tokenItem)

		val request = mapOf("refreshToken" to "existing_refresh_token")

		// when & then
		assertThat(
			mvc.post().uri("/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
		)
			.hasStatusOk()
			.apply(
				document(
					"admin/auth/refresh",
					preprocessRequest(prettyPrint()),
					preprocessResponse(prettyPrint()),
					requestFields(
						fieldWithPath("refreshToken").type(JsonFieldType.STRING).description("리프레시 토큰")
					),
					responseFields(
						fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
						fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
						fieldWithPath("data.accessToken").type(JsonFieldType.STRING).description("새 액세스 토큰"),
						fieldWithPath("data.refreshToken").type(JsonFieldType.STRING).description("새 리프레시 토큰"),
						fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
						fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
						fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
						fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
						fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
					)
				)
			)
	}

	@Test
	@DisplayName("어드민 탈퇴")
	fun withdraw() {
		// given
		Mockito.mockStatic(SecurityContextUtil::class.java).use { mockedStatic: MockedStatic<SecurityContextUtil> ->
			mockedStatic.`when`<Optional<Long>> { SecurityContextUtil.getAdminUserIdByContext() }
				.thenReturn(Optional.of(1L))

			// when & then
			assertThat(
				mvc.delete().uri("/auth/withdraw")
					.header("Authorization", "Bearer test_access_token")
			)
				.hasStatusOk()
				.apply(
					document(
						"admin/auth/withdraw",
						preprocessRequest(prettyPrint()),
						preprocessResponse(prettyPrint()),
						requestHeaders(
							headerWithName("Authorization").description("Bearer 액세스 토큰")
						),
						responseFields(
							fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
							fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
							fieldWithPath("data.message").type(JsonFieldType.STRING).description("처리 결과 메시지"),
							fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
							fieldWithPath("meta.serverVersion").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverEncoding").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverResponseTime").type(JsonFieldType.STRING).ignored(),
							fieldWithPath("meta.serverPathVersion").type(JsonFieldType.STRING).ignored()
						)
					)
				)
		}
	}

	@Test
	@DisplayName("어드민 회원가입")
	fun signup() {
		// given
		val response = AdminSignupResponse(
			1L,
			"new@bottlenote.com",
			"새 어드민",
			listOf(AdminRole.PARTNER, AdminRole.COMMUNITY_MANAGER)
		)
		given(authService.signup(anyLong(), org.mockito.ArgumentMatchers.any(AdminSignupRequest::class.java))).willReturn(response)

		Mockito.mockStatic(SecurityContextUtil::class.java).use { mockedStatic: MockedStatic<SecurityContextUtil> ->
			mockedStatic.`when`<Optional<Long>> { SecurityContextUtil.getAdminUserIdByContext() }
				.thenReturn(Optional.of(1L))

			val request = mapOf(
				"email" to "new@bottlenote.com",
				"password" to "password123",
				"name" to "새 어드민",
				"roles" to listOf("PARTNER", "COMMUNITY_MANAGER")
			)

			// when & then
			assertThat(
				mvc.post().uri("/auth/signup")
					.header("Authorization", "Bearer test_access_token")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.apply(
					document(
						"admin/auth/signup",
						preprocessRequest(prettyPrint()),
						preprocessResponse(prettyPrint()),
						requestHeaders(
							headerWithName("Authorization").description("Bearer 액세스 토큰")
						),
						requestFields(
							fieldWithPath("email").type(JsonFieldType.STRING).description("어드민 이메일"),
							fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호 (8~35자)"),
							fieldWithPath("name").type(JsonFieldType.STRING).description("어드민 이름 (2~50자)"),
							fieldWithPath("roles").type(JsonFieldType.ARRAY).description("역할 목록 (ROOT_ADMIN, PARTNER, CLIENT, BAR_OWNER, COMMUNITY_MANAGER)")
						),
						responseFields(
							fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
							fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드"),
							fieldWithPath("data.adminId").type(JsonFieldType.NUMBER).description("생성된 어드민 ID"),
							fieldWithPath("data.email").type(JsonFieldType.STRING).description("어드민 이메일"),
							fieldWithPath("data.name").type(JsonFieldType.STRING).description("어드민 이름"),
							fieldWithPath("data.roles").type(JsonFieldType.ARRAY).description("부여된 역할 목록"),
							fieldWithPath("errors").type(JsonFieldType.ARRAY).description("에러 목록"),
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
