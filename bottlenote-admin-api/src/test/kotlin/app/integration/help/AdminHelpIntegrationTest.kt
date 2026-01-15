package app.integration.help

import app.IntegrationTestSupport
import app.bottlenote.support.constant.StatusType
import app.bottlenote.support.help.constant.HelpType
import app.bottlenote.support.help.fixture.HelpTestFactory
import app.bottlenote.user.fixture.UserTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

@Tag("admin_integration")
@DisplayName("[integration] Admin Help API 통합 테스트")
class AdminHelpIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var helpTestFactory: HelpTestFactory

	@Autowired
	private lateinit var userTestFactory: UserTestFactory

	private lateinit var accessToken: String

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	@Nested
	@DisplayName("문의 목록 조회 API")
	inner class GetHelpListTest {

		@Test
		@DisplayName("문의 목록을 조회할 수 있다")
		fun getHelpList() {
			// given
			val user = userTestFactory.persistUser()
			helpTestFactory.persistMultipleHelpsByUser(user.id, HelpType.WHISKEY, 5)

			// when & then
			assertThat(
				mockMvcTester.get().uri("/helps")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)
		}

		@Test
		@DisplayName("상태로 필터링하여 조회할 수 있다")
		fun getHelpListFilterByStatus() {
			// given
			val user = userTestFactory.persistUser()
			helpTestFactory.persistHelp(user.id, HelpType.WHISKEY)

			// when & then
			assertThat(
				mockMvcTester.get().uri("/helps")
					.header("Authorization", "Bearer $accessToken")
					.param("status", StatusType.WAITING.name)
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)
		}

		@Test
		@DisplayName("문의 유형으로 필터링하여 조회할 수 있다")
		fun getHelpListFilterByType() {
			// given
			val user = userTestFactory.persistUser()
			helpTestFactory.persistHelp(user.id, HelpType.WHISKEY)
			helpTestFactory.persistHelp(user.id, HelpType.REVIEW)

			// when & then
			assertThat(
				mockMvcTester.get().uri("/helps")
					.header("Authorization", "Bearer $accessToken")
					.param("type", HelpType.WHISKEY.name)
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)
		}

		@Test
		@DisplayName("인증 없이 조회하면 실패한다")
		fun getHelpListWithoutAuth() {
			// when & then
			assertThat(
				mockMvcTester.get().uri("/helps")
			)
				.hasStatus4xxClientError()
		}
	}

	@Nested
	@DisplayName("문의 상세 조회 API")
	inner class GetHelpDetailTest {

		@Test
		@DisplayName("문의 상세를 조회할 수 있다")
		fun getHelpDetail() {
			// given
			val user = userTestFactory.persistUser()
			val help = helpTestFactory.persistHelp(user.id, HelpType.WHISKEY, "테스트 제목", "테스트 내용")

			// when & then
			assertThat(
				mockMvcTester.get().uri("/helps/${help.id}")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)

			assertThat(
				mockMvcTester.get().uri("/helps/${help.id}")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.title").isEqualTo("테스트 제목")
		}

		@Test
		@DisplayName("존재하지 않는 문의를 조회하면 실패한다")
		fun getHelpDetailNotFound() {
			// when & then
			assertThat(
				mockMvcTester.get().uri("/helps/99999")
					.header("Authorization", "Bearer $accessToken")
			)
				.hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(false)
		}
	}

	@Nested
	@DisplayName("문의 답변 등록 API")
	inner class AnswerHelpTest {

		@Test
		@DisplayName("문의에 답변을 등록할 수 있다")
		fun answerHelp() {
			// given
			val user = userTestFactory.persistUser()
			val help = helpTestFactory.persistHelp(user.id, HelpType.WHISKEY)

			val request = mapOf(
				"responseContent" to "답변 내용입니다.",
				"status" to StatusType.SUCCESS.name
			)

			// when & then
			assertThat(
				mockMvcTester.post().uri("/helps/${help.id}/answer")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(true)

			assertThat(
				mockMvcTester.post().uri("/helps/${help.id}/answer")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.status").isEqualTo(StatusType.SUCCESS.name)
		}

		@Test
		@DisplayName("반려 상태로 답변을 등록할 수 있다")
		fun answerHelpWithReject() {
			// given
			val user = userTestFactory.persistUser()
			val help = helpTestFactory.persistHelp(user.id, HelpType.WHISKEY)

			val request = mapOf(
				"responseContent" to "반려 사유입니다.",
				"status" to StatusType.REJECT.name
			)

			// when & then
			assertThat(
				mockMvcTester.post().uri("/helps/${help.id}/answer")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatusOk()
				.bodyJson()
				.extractingPath("$.data.status").isEqualTo(StatusType.REJECT.name)
		}

		@Test
		@DisplayName("존재하지 않는 문의에 답변하면 실패한다")
		fun answerHelpNotFound() {
			// given
			val request = mapOf(
				"responseContent" to "답변 내용입니다.",
				"status" to StatusType.SUCCESS.name
			)

			// when & then
			assertThat(
				mockMvcTester.post().uri("/helps/99999/answer")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
				.bodyJson()
				.extractingPath("$.success").isEqualTo(false)
		}

		@Test
		@DisplayName("답변 내용 없이 요청하면 실패한다")
		fun answerHelpWithoutContent() {
			// given
			val user = userTestFactory.persistUser()
			val help = helpTestFactory.persistHelp(user.id, HelpType.WHISKEY)

			val request = mapOf(
				"responseContent" to "",
				"status" to StatusType.SUCCESS.name
			)

			// when & then
			assertThat(
				mockMvcTester.post().uri("/helps/${help.id}/answer")
					.header("Authorization", "Bearer $accessToken")
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
			)
				.hasStatus4xxClientError()
		}
	}
}
