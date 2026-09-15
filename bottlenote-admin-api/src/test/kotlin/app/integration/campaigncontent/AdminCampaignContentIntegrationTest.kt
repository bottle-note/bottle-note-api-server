package app.integration.campaigncontent

import app.IntegrationTestSupport
import app.bottlenote.campaigncontent.constant.CampaignContentEventType
import app.bottlenote.campaigncontent.constant.CampaignContentEventType.FINISH
import app.bottlenote.campaigncontent.constant.CampaignContentEventType.RESULT
import app.bottlenote.campaigncontent.constant.CampaignContentEventType.START
import app.bottlenote.campaigncontent.constant.CampaignContentEventType.VIEW
import app.bottlenote.campaigncontent.fixture.CampaignContentTestFactory
import app.bottlenote.statistics.fixture.VisitorTelemetryTestFactory
import app.bottlenote.user.fixture.UserTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MvcTestResult
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Tag("admin_integration")
@DisplayName("[integration] Admin 캠페인 콘텐츠 API 통합 테스트")
class AdminCampaignContentIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var campaignContentTestFactory: CampaignContentTestFactory

	@Autowired
	private lateinit var visitorTelemetryTestFactory: VisitorTelemetryTestFactory

	@Autowired
	private lateinit var userTestFactory: UserTestFactory

	private lateinit var accessToken: String
	private val zone: ZoneId = ZoneId.of("Asia/Seoul")

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	@Nested
	@DisplayName("등록·조회·수정·삭제")
	inner class Management {

		@Test
		@DisplayName("캠페인 콘텐츠를 등록하면 목록과 상세에서 조회할 수 있다")
		fun createAndRead() {
			val created = exchange(
				mockMvcTester.post().uri("/v1/campaign-contents")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"code":"whiskey-mbti","name":"위스키 MBTI","description":"술자리 성향 테스트"}""")
			)

			assertThat(created).hasStatusOk()
			assertThat(created).bodyJson().extractingPath("$.data.code").isEqualTo("CAMPAIGN_CONTENT_CREATED")
			val id = extractTargetId(created)

			val list = exchange(mockMvcTester.get().uri("/v1/campaign-contents").param("keyword", "mbti"))
			assertThat(list).hasStatusOk()
			assertThat(list).bodyJson().extractingPath("$.data[0].code").isEqualTo("whiskey-mbti")
			assertThat(list).bodyJson().extractingPath("$.data[0].recentParticipants").isEqualTo(0)
			assertThat(list).bodyJson().extractingPath("$.meta.totalElements").isEqualTo(1)

			val detail = exchange(mockMvcTester.get().uri("/v1/campaign-contents/$id"))
			assertThat(detail).hasStatusOk()
			assertThat(detail).bodyJson().extractingPath("$.data.name").isEqualTo("위스키 MBTI")
			assertThat(detail).bodyJson().extractingPath("$.data.isActive").isEqualTo(true)
		}

		@Test
		@DisplayName("이미 쓰는 코드로 등록하면 409로 실패한다")
		fun createDuplicateCode() {
			campaignContentTestFactory.persistCampaignContent("whiskey-mbti", "위스키 MBTI")

			val result = exchange(
				mockMvcTester.post().uri("/v1/campaign-contents")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"code":"whiskey-mbti","name":"다른 이름"}""")
			)

			assertThat(result).hasStatus(HttpStatus.CONFLICT)
		}

		@Test
		@DisplayName("형식에 맞지 않는 코드로 등록하면 400으로 실패한다")
		fun createInvalidCode() {
			val result = exchange(
				mockMvcTester.post().uri("/v1/campaign-contents")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"code":"Whiskey_MBTI","name":"위스키 MBTI"}""")
			)

			assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
		}

		@Test
		@DisplayName("수정하면 이름·설명·활성 여부가 바뀌고 코드는 유지된다")
		fun updateKeepsCode() {
			val content = campaignContentTestFactory.persistCampaignContent("whiskey-mbti", "위스키 MBTI")

			val result = exchange(
				mockMvcTester.put().uri("/v1/campaign-contents/${content.id}")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"name":"새 이름","description":"새 설명","isActive":false,"code":"changed"}""")
			)

			assertThat(result).hasStatusOk()
			val detail = exchange(mockMvcTester.get().uri("/v1/campaign-contents/${content.id}"))
			assertThat(detail).bodyJson().extractingPath("$.data.code").isEqualTo("whiskey-mbti")
			assertThat(detail).bodyJson().extractingPath("$.data.name").isEqualTo("새 이름")
			assertThat(detail).bodyJson().extractingPath("$.data.isActive").isEqualTo(false)
		}

		@Test
		@DisplayName("활성 상태를 변경할 수 있다")
		fun updateStatus() {
			val content = campaignContentTestFactory.persistCampaignContent("whiskey-mbti", "위스키 MBTI")

			val result = exchange(
				mockMvcTester.patch().uri("/v1/campaign-contents/${content.id}/status")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"isActive":false}""")
			)

			assertThat(result).hasStatusOk()
			val list = exchange(mockMvcTester.get().uri("/v1/campaign-contents").param("isActive", "false"))
			assertThat(list).bodyJson().extractingPath("$.data[0].id").isEqualTo(content.id!!.toInt())
		}

		@Test
		@DisplayName("참여 기록이 있으면 삭제가 409로 실패하고, 없으면 삭제된다")
		fun deleteOnlyWithoutEvents() {
			val used = campaignContentTestFactory.persistCampaignContent("whiskey-mbti", "위스키 MBTI")
			val unused = campaignContentTestFactory.persistCampaignContent("whiskey-tarot", "위스키 타로")
			campaignContentTestFactory.persistEvent(used.id!!, VIEW, visitor("A"), null, "203.0.113.10", LocalDateTime.now(zone))

			val rejected = exchange(mockMvcTester.delete().uri("/v1/campaign-contents/${used.id}"))
			val deleted = exchange(mockMvcTester.delete().uri("/v1/campaign-contents/${unused.id}"))

			assertThat(rejected).hasStatus(HttpStatus.CONFLICT)
			assertThat(deleted).hasStatusOk()
			assertThat(exchange(mockMvcTester.get().uri("/v1/campaign-contents/${unused.id}"))).hasStatus(HttpStatus.NOT_FOUND)
		}
	}

	@Nested
	@DisplayName("참여 지표")
	inner class Metrics {

		@Test
		@DisplayName("방문자 통계 제외 규칙을 적용해 순방문자·순회원과 비율을 계산한다")
		fun getMetrics() {
			val content = campaignContentTestFactory.persistCampaignContent("whiskey-mbti", "위스키 MBTI")
			val member = userTestFactory.persistUser()
			val otherMember = userTestFactory.persistUser()
			val today = LocalDate.now(zone)
			val at = today.atStartOfDay().plusMinutes(1)

			// A는 끝까지 참여, B는 시작만, C는 완료 후 로그인하지 않음
			events(content.id!!, "A", at, VIEW, START, FINISH)
			campaignContentTestFactory.persistEvent(content.id!!, RESULT, visitor("A"), member.id, "203.0.113.10", at)
			events(content.id!!, "B", at, VIEW, START)
			events(content.id!!, "C", at, VIEW, START, FINISH)
			// 봇과 제외 IP 대역은 지표에서 빠진다
			campaignContentTestFactory.persistEvent(content.id!!, VIEW, visitor("D"), null, "203.0.113.20", "봇", at)
			campaignContentTestFactory.persistEvent(content.id!!, VIEW, visitor("E"), null, "66.249.1.1", at)
			// 같은 기간 서비스 전체 활성 회원 2명
			visitorTelemetryTestFactory.persistEvent(at, visitor("A"), member.id, "203.0.113.10", "모바일")
			visitorTelemetryTestFactory.persistEvent(at, visitor("F"), otherMember.id, "203.0.113.11", "모바일")

			val result = exchange(
				mockMvcTester.get().uri("/v1/campaign-contents/${content.id}/metrics")
					.param("from", today.toString())
					.param("to", today.toString())
			)

			assertThat(result).hasStatusOk()
			val json = assertThat(result).bodyJson()
			json.extractingPath("$.data.viewVisitors").isEqualTo(3)
			json.extractingPath("$.data.startVisitors").isEqualTo(3)
			json.extractingPath("$.data.finishVisitors").isEqualTo(2)
			json.extractingPath("$.data.resultMembers").isEqualTo(1)
			json.extractingPath("$.data.activeMembers").isEqualTo(2)
			json.extractingPath("$.data.completionRate").isEqualTo(66.7)
			json.extractingPath("$.data.loginConversionRate").isEqualTo(50.0)
			json.extractingPath("$.data.participationRate").isEqualTo(50.0)

			val list = exchange(mockMvcTester.get().uri("/v1/campaign-contents"))
			assertThat(list).bodyJson().extractingPath("$.data[0].recentParticipants").isEqualTo(1)
		}

		@Test
		@DisplayName("보존 기간 90일을 벗어난 기간이면 400으로 실패한다")
		fun getMetricsOutOfRetention() {
			val content = campaignContentTestFactory.persistCampaignContent("whiskey-mbti", "위스키 MBTI")
			val today = LocalDate.now(zone)

			val result = exchange(
				mockMvcTester.get().uri("/v1/campaign-contents/${content.id}/metrics")
					.param("from", today.minusDays(90).toString())
					.param("to", today.toString())
			)

			assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
		}
	}

	private fun events(
		campaignContentId: Long,
		visitorKey: String,
		at: LocalDateTime,
		vararg types: CampaignContentEventType
	) {
		types.forEach { type ->
			campaignContentTestFactory.persistEvent(campaignContentId, type, visitor(visitorKey), null, "203.0.113.10", at)
		}
	}

	private fun visitor(key: String): String = key.repeat(64)

	private fun exchange(request: org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder): MvcTestResult = request.header("Authorization", "Bearer $accessToken").exchange()

	private fun extractTargetId(result: MvcTestResult): Long {
		val data = parseResponse(result).data as Map<*, *>
		return (data["targetId"] as Number).toLong()
	}
}
