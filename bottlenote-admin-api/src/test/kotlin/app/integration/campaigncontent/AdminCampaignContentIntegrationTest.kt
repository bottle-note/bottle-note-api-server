package app.integration.campaigncontent

import app.IntegrationTestSupport
import app.bottlenote.campaigncontent.constant.CampaignContentEventType
import app.bottlenote.campaigncontent.constant.CampaignContentEventType.FINISH
import app.bottlenote.campaigncontent.constant.CampaignContentEventType.RESULT
import app.bottlenote.campaigncontent.constant.CampaignContentEventType.START
import app.bottlenote.campaigncontent.constant.CampaignContentEventType.VIEW
import app.bottlenote.campaigncontent.domain.CampaignContentEventRepository
import app.bottlenote.campaigncontent.domain.CampaignContentMetricsRepository
import app.bottlenote.campaigncontent.domain.CampaignContentRepository
import app.bottlenote.campaigncontent.dto.request.AdminCampaignContentCreateRequest
import app.bottlenote.campaigncontent.exception.CampaignContentExceptionCode
import app.bottlenote.campaigncontent.fixture.CampaignContentTestFactory
import app.bottlenote.campaigncontent.repository.JpaCampaignContentEventRepository
import app.bottlenote.campaigncontent.repository.JpaCampaignContentRepository
import app.bottlenote.campaigncontent.service.AdminCampaignContentService
import app.bottlenote.statistics.facade.VisitorStatisticsFacade
import app.bottlenote.statistics.fixture.VisitorTelemetryTestFactory
import app.bottlenote.user.fixture.UserTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MvcTestResult
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
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

	@Autowired
	private lateinit var campaignContentRepository: JpaCampaignContentRepository

	@Autowired
	private lateinit var campaignContentEventRepository: JpaCampaignContentEventRepository

	@Autowired
	private lateinit var campaignContentMetricsRepository: CampaignContentMetricsRepository

	@Autowired
	private lateinit var visitorStatisticsFacade: VisitorStatisticsFacade

	@Autowired
	private lateinit var clock: Clock

	@Autowired
	private lateinit var transactionManager: PlatformTransactionManager

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
		@DisplayName("사전 중복 확인 뒤 DB unique 제약이 발생해도 DUPLICATE_CODE로 변환한다")
		fun createDuplicateCodeFromDatabaseConstraint() {
			campaignContentTestFactory.persistCampaignContent("whiskey-mbti", "위스키 MBTI")
			val repositoryWithoutPrecheck = object : CampaignContentRepository by campaignContentRepository {
				override fun existsByCode(code: String): Boolean = false
			}
			val service = service(repositoryWithoutPrecheck, campaignContentEventRepository)

			assertThatThrownBy {
				TransactionTemplate(transactionManager).executeWithoutResult {
					service.create(AdminCampaignContentCreateRequest("whiskey-mbti", "다른 이름", null, true))
				}
			}.extracting("exceptionCode")
				.isEqualTo(CampaignContentExceptionCode.CAMPAIGN_CONTENT_DUPLICATE_CODE)
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

		@Test
		@DisplayName("이벤트 사전 확인 뒤 DB FK 제약이 발생해도 HAS_EVENTS로 변환한다")
		fun deleteFromDatabaseConstraint() {
			val content = campaignContentTestFactory.persistCampaignContent("whiskey-mbti", "위스키 MBTI")
			campaignContentTestFactory.persistEvent(content.id!!, VIEW, visitor("A"), null, "203.0.113.10", LocalDateTime.now(clock))
			val eventRepositoryWithoutPrecheck = object : CampaignContentEventRepository by campaignContentEventRepository {
				override fun existsByCampaignContentId(campaignContentId: Long?): Boolean = false
			}
			val service = service(campaignContentRepository, eventRepositoryWithoutPrecheck)

			assertThatThrownBy {
				TransactionTemplate(transactionManager).executeWithoutResult {
					service.delete(content.id!!)
				}
			}.extracting("exceptionCode")
				.isEqualTo(CampaignContentExceptionCode.CAMPAIGN_CONTENT_HAS_EVENTS)
		}
	}

	@Nested
	@DisplayName("참여 지표")
	inner class Metrics {

		@Test
		@DisplayName("방문자 통계 제외 규칙을 적용해 순방문자·순회원과 비율을 계산한다")
		fun getMetrics() {
			val content = campaignContentTestFactory.persistCampaignContent("whiskey-mbti", "위스키 MBTI")
			val memberA = userTestFactory.persistUser()
			val memberE = userTestFactory.persistUser()
			val repeatedMember = userTestFactory.persistUser()
			val activeOnlyMember = userTestFactory.persistUser()
			val rootAdminMember = userTestFactory.persistUserAsRootAdmin()
			val excludedMember = userTestFactory.persistUser()
			val botMember = userTestFactory.persistUser()
			val excludedIpMember = userTestFactory.persistUser()
			val outsideMember = userTestFactory.persistUser()
			val today = LocalDate.now(zone)
			val at = today.atStartOfDay().plusMinutes(1)
			val previousDay = today.minusDays(1).atStartOfDay().plusMinutes(1)
			val outside = today.minusDays(8).atStartOfDay().plusMinutes(1)

			// B는 START만, D는 START 없이 FINISH만, E는 RESULT만 있다.
			// 유효 집합: VIEW A/B/C=3, START A/B/C=3, FINISH A/C/D=3, RESULT 회원 A/E/F=3
			events(content.id!!, "A", at, VIEW, START, FINISH)
			campaignContentTestFactory.persistEvent(content.id!!, RESULT, visitor("A"), memberA.id, "203.0.113.10", at)
			events(content.id!!, "B", at, VIEW, START)
			events(content.id!!, "C", at, VIEW, START, FINISH)
			events(content.id!!, "D", at, FINISH)
			campaignContentTestFactory.persistEvent(content.id!!, RESULT, visitor("E"), memberE.id, "203.0.113.10", at)
			campaignContentTestFactory.persistEvent(content.id!!, RESULT, visitor("F"), repeatedMember.id, "203.0.113.10", at)
			campaignContentTestFactory.persistEvent(content.id!!, RESULT, visitor("F"), repeatedMember.id, "203.0.113.10", previousDay)
			campaignContentTestFactory.persistEvent(content.id!!, RESULT, visitor("G"), repeatedMember.id, "203.0.113.10", previousDay.plusSeconds(1))

			// 기간 밖·root_admins·NULL IP·도구·봇·제외 IP 이벤트는 분자에서 제외한다.
			campaignContentTestFactory.persistEvent(content.id!!, RESULT, visitor("O"), outsideMember.id, "203.0.113.10", outside)
			campaignContentTestFactory.persistEvent(content.id!!, RESULT, visitor("R"), rootAdminMember.id, "203.0.113.10", at)
			campaignContentTestFactory.persistEvent(content.id!!, RESULT, visitor("N"), excludedMember.id, null, at)
			campaignContentTestFactory.persistEvent(content.id!!, RESULT, visitor("T"), excludedMember.id, "203.0.113.10", "도구", at)
			campaignContentTestFactory.persistEvent(content.id!!, RESULT, visitor("I"), botMember.id, "203.0.113.18", "봇", at)
			campaignContentTestFactory.persistEvent(content.id!!, RESULT, visitor("J"), excludedIpMember.id, "66.249.1.1", at)

			// 분모도 봇·66.249. 회원을 제외하며, F는 날짜별 DISTINCT 합이 아니라 기간 전체에서 한 번 센다.
			// 유효 회원 A/E/F와 캠페인 미참여 회원 H까지 DISTINCT 4명이다.
			// 완주율 2/3=66.7, 로그인 전환율 1/3=33.3, 참여율 3/4=75.0이다.
			visitorTelemetryTestFactory.persistEvent(at, visitor("A"), memberA.id, "203.0.113.10", "모바일")
			visitorTelemetryTestFactory.persistEvent(at, visitor("E"), memberE.id, "203.0.113.11", "모바일")
			visitorTelemetryTestFactory.persistEvent(at, visitor("F"), repeatedMember.id, "203.0.113.12", "모바일")
			visitorTelemetryTestFactory.persistEvent(at, visitor("G"), repeatedMember.id, "203.0.113.13", "모바일")
			visitorTelemetryTestFactory.persistEvent(at, visitor("H"), activeOnlyMember.id, "203.0.113.14", "모바일")
			visitorTelemetryTestFactory.persistEvent(at, visitor("R"), rootAdminMember.id, "203.0.113.15", "모바일")
			visitorTelemetryTestFactory.persistEvent(at, visitor("N"), excludedMember.id, null, "모바일")
			visitorTelemetryTestFactory.persistEvent(at, visitor("T"), excludedMember.id, "203.0.113.16", "도구")
			visitorTelemetryTestFactory.persistEvent(at, visitor("I"), botMember.id, "203.0.113.18", "봇")
			visitorTelemetryTestFactory.persistEvent(at, visitor("J"), excludedIpMember.id, "66.249.1.1", "모바일")
			visitorTelemetryTestFactory.persistEvent(previousDay, visitor("F"), repeatedMember.id, "203.0.113.12", "모바일")
			visitorTelemetryTestFactory.persistEvent(outside, visitor("O"), outsideMember.id, "203.0.113.17", "모바일")

			val result = exchange(
				mockMvcTester.get().uri("/v1/campaign-contents/${content.id}/metrics")
					.param("from", today.minusDays(1).toString())
					.param("to", today.toString())
			)

			assertThat(result).hasStatusOk()
			val json = assertThat(result).bodyJson()
			json.extractingPath("$.data.viewVisitors").isEqualTo(3)
			json.extractingPath("$.data.startVisitors").isEqualTo(3)
			json.extractingPath("$.data.finishVisitors").isEqualTo(3)
			json.extractingPath("$.data.resultMembers").isEqualTo(3)
			json.extractingPath("$.data.activeMembers").isEqualTo(4)
			json.extractingPath("$.data.completionRate").isEqualTo(66.7)
			json.extractingPath("$.data.loginConversionRate").isEqualTo(33.3)
			json.extractingPath("$.data.participationRate").isEqualTo(75.0)

			val list = exchange(mockMvcTester.get().uri("/v1/campaign-contents"))
			assertThat(list).bodyJson().extractingPath("$.data[0].recentParticipants").isEqualTo(3)
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

	private fun service(
		contentRepository: CampaignContentRepository,
		eventRepository: CampaignContentEventRepository
	): AdminCampaignContentService = AdminCampaignContentService(
		contentRepository,
		eventRepository,
		campaignContentMetricsRepository,
		visitorStatisticsFacade,
		clock
	)

	private fun exchange(request: org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder): MvcTestResult = request.header("Authorization", "Bearer $accessToken").exchange()

	private fun extractTargetId(result: MvcTestResult): Long {
		val data = parseResponse(result).data as Map<*, *>
		return (data["targetId"] as Number).toLong()
	}
}
