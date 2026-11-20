package app.integration.mfds

import app.IntegrationTestSupport
import app.bottlenote.alcohols.constant.AlcoholType
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.mfds.constant.MfdsNormalizationStatus
import app.bottlenote.mfds.domain.MfdsDeclaration
import app.bottlenote.mfds.domain.MfdsDeclarationRepository
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingConfirmRequest
import app.bottlenote.mfds.fixture.MfdsTestFactory
import app.bottlenote.mfds.repository.JpaMfdsDeclarationRepository
import app.bottlenote.mfds.service.MfdsBulkMatchingService
import app.bottlenote.user.constant.UserType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import java.util.concurrent.atomic.AtomicInteger

@Tag("admin_integration")
@DisplayName("[integration] MFDS 일괄 매칭 인증·반영·롤백")
@Import(AdminMfdsBulkMatchingIntegrationTest.GatedRepositoryConfiguration::class)
class AdminMfdsBulkMatchingIntegrationTest : IntegrationTestSupport() {
	@Autowired private lateinit var mfdsTestFactory: MfdsTestFactory

	@Autowired private lateinit var alcoholTestFactory: AlcoholTestFactory

	@Autowired private lateinit var bulkService: MfdsBulkMatchingService

	@Autowired private lateinit var jdbcTemplate: JdbcTemplate

	@Autowired private lateinit var gated: GatedDeclarationRepository

	private lateinit var accessToken: String
	private var adminId: Long = 0

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		adminId = admin.id
		accessToken = getAccessToken(admin)
	}

	@AfterEach
	fun resetHooks() {
		gated.beforeSave = {}
	}

	@Test
	@DisplayName("인증이 없으면 일괄 미리보기를 거절한다")
	fun previewRequiresAuthentication() {
		val result = mockMvcTester.post().uri("/v1/mfds/declarations/1/matching/bulk-preview")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""{"alcoholId":1}""")
			.exchange()
		assertThat(result).hasStatus(HttpStatus.FORBIDDEN)
	}

	@Test
	@DisplayName("인증이 없으면 일괄 확정을 거절한다")
	fun confirmRequiresAuthentication() {
		val result = mockMvcTester.post().uri("/v1/mfds/declarations/1/matching/bulk-confirm")
			.contentType(MediaType.APPLICATION_JSON)
			.content(confirmBody(1L, listOf(1L)))
			.exchange()
		assertThat(result).hasStatus(HttpStatus.FORBIDDEN)
	}

	@Test
	@DisplayName("일반 사용자 토큰은 일괄 미리보기와 확정을 거절한다")
	fun productUserTokenIsRejected() {
		val token = jwtTokenProvider.generateToken("member@example.com", UserType.ROLE_USER, 99L).accessToken()
		val preview = mockMvcTester.post().uri("/v1/mfds/declarations/1/matching/bulk-preview")
			.header("Authorization", "Bearer $token")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""{"alcoholId":1}""")
			.exchange()
		val confirm = mockMvcTester.post().uri("/v1/mfds/declarations/1/matching/bulk-confirm")
			.header("Authorization", "Bearer $token")
			.contentType(MediaType.APPLICATION_JSON)
			.content(confirmBody(1L, listOf(1L)))
			.exchange()
		assertThat(preview).hasStatus(HttpStatus.FORBIDDEN)
		assertThat(confirm).hasStatus(HttpStatus.FORBIDDEN)
	}

	@Test
	@DisplayName("관리자 토큰으로 잘못된 요청을 보내면 권한 오류가 아니라 입력 오류로 거절한다")
	fun adminTokenPassesAuthentication() {
		val missingAlcohol = mockMvcTester.post().uri("/v1/mfds/declarations/1/matching/bulk-preview")
			.header("Authorization", "Bearer $accessToken")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""{"alcoholId":1}""")
			.exchange()
		val emptySelection = mockMvcTester.post().uri("/v1/mfds/declarations/1/matching/bulk-confirm")
			.header("Authorization", "Bearer $accessToken")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""{"alcoholId":1,"declarationIds":[]}""")
			.exchange()
		assertThat(missingAlcohol).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(emptySelection).hasStatus(HttpStatus.BAD_REQUEST)
	}

	@Test
	@DisplayName("미리보기 없이 확정할 때 기존 연결을 덮어쓰고 같은 연결은 건너뛴다")
	fun confirmOverwritesAndSkipsSameLink() {
		val alcohol = alcoholTestFactory.persistAlcohol("확정 주류", "Confirmed", AlcoholType.WHISKY)
		val existing = alcoholTestFactory.persistAlcohol("기존 주류", "Existing", AlcoholType.WHISKY)
		val linked = declaration("RCNO-OVERWRITE")
		val same = declaration("RCNO-SAME")
		jdbcTemplate.update("update mfds_declarations set selected_alcohol_id = ? where id = ?", existing.id!!, linked.id!!)
		jdbcTemplate.update(
			"update mfds_declarations set selected_alcohol_id = ?, selected_distillery_id = ?, selected_region_id = ? where id = ?",
			alcohol.id!!,
			alcohol.distillery?.id,
			alcohol.region?.id,
			same.id!!
		)

		val result = mockMvcTester.post().uri("/v1/mfds/declarations/${linked.id}/matching/bulk-confirm")
			.header("Authorization", "Bearer $accessToken")
			.contentType(MediaType.APPLICATION_JSON)
			.content(confirmBody(alcohol.id!!, listOf(same.id!!, linked.id!!)))
			.exchange()

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.applied[0].declarationId").asNumber().isEqualTo(linked.id!!.toInt())
		assertThat(result).bodyJson().extractingPath("$.data.unchangedDeclarationIds[0]").asNumber().isEqualTo(same.id!!.toInt())
		assertThat(selected(linked.id!!)).isEqualTo(alcohol.id)
		assertThat(selectionCount(same.id!!)).isZero()
	}

	@Test
	@DisplayName("없는 신고가 섞여 있으면 404로 거절하고 아무것도 쓰지 않는다")
	fun missingDeclarationRejectsAll() {
		val alcohol = alcoholTestFactory.persistAlcohol("거절 주류", "Rejected", AlcoholType.WHISKY)
		val declaration = declaration("RCNO-MISSING")

		val result = mockMvcTester.post().uri("/v1/mfds/declarations/${declaration.id}/matching/bulk-confirm")
			.header("Authorization", "Bearer $accessToken")
			.contentType(MediaType.APPLICATION_JSON)
			.content(confirmBody(alcohol.id!!, listOf(declaration.id!!, 999_999L)))
			.exchange()

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND)
		assertThat(selected(declaration.id!!)).isNull()
	}

	@Test
	@DisplayName("두 번째 저장이 실패하면 본문과 감사 이력이 함께 롤백된다")
	fun secondSaveRollsBackEverything() {
		val alcohol = alcoholTestFactory.persistAlcohol("롤백 주류", "Rollback", AlcoholType.WHISKY)
		val first = declaration("RCNO-RB1")
		val second = declaration("RCNO-RB2")
		val saves = AtomicInteger()
		gated.beforeSave = { if (saves.incrementAndGet() == 2) throw IllegalStateException("nth save") }

		assertThatThrownBy {
			bulkService.confirm(
				first.id!!,
				MfdsBulkMatchingConfirmRequest(alcohol.id!!, null, null, listOf(first.id!!, second.id!!)),
				adminId
			)
		}.isInstanceOf(IllegalStateException::class.java)

		assertThat(selected(first.id!!)).isNull()
		assertThat(selected(second.id!!)).isNull()
		assertThat(selectionCount(first.id!!) + selectionCount(second.id!!)).isZero()
	}

	private fun declaration(rcno: String): MfdsDeclaration = mfdsTestFactory.persistDeclaration(rcno, MfdsNormalizationStatus.NORMALIZED, null, null, null)

	private fun confirmBody(
		alcoholId: Long,
		ids: List<Long>
	) = """{"alcoholId":$alcoholId,"declarationIds":${ids.joinToString(",", "[", "]")}}"""

	private fun selected(id: Long): Long? = jdbcTemplate.query(
		"select selected_alcohol_id from mfds_declarations where id = ?",
		{ rs, _ -> rs.getLong(1).takeUnless { rs.wasNull() } },
		id
	).single()

	private fun selectionCount(id: Long): Long = jdbcTemplate.queryForObject(
		"select count(*) from mfds_matching_selections where declaration_id = ?",
		Long::class.java,
		id
	)!!

	/** 운영 훅 없이 기존 포트를 감싸 N번째 저장 실패를 만든다. */
	class GatedDeclarationRepository(private val delegate: MfdsDeclarationRepository) : MfdsDeclarationRepository by delegate {
		@Volatile var beforeSave: () -> Unit = {}

		override fun save(declaration: MfdsDeclaration): MfdsDeclaration {
			beforeSave()
			return delegate.save(declaration)
		}
	}

	@TestConfiguration(proxyBeanMethods = false)
	class GatedRepositoryConfiguration {
		@Bean
		@Primary
		fun gatedDeclarationRepository(delegate: JpaMfdsDeclarationRepository) = GatedDeclarationRepository(delegate)
	}
}
