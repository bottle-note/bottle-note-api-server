package app.integration.mfds

import app.IntegrationTestSupport
import app.bottlenote.alcohols.constant.AlcoholType
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.mfds.constant.MfdsNormalizationStatus
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingConfirmRequest
import app.bottlenote.mfds.dto.request.MfdsBulkMatchingPreviewRequest
import app.bottlenote.mfds.exception.MfdsException
import app.bottlenote.mfds.fixture.MfdsTestFactory
import app.bottlenote.mfds.service.MfdsBulkLockGate
import app.bottlenote.mfds.service.MfdsBulkMatchingService
import app.bottlenote.mfds.service.MfdsBulkSaveGuard
import app.bottlenote.user.constant.UserType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@Tag("admin_integration")
@DisplayName("[integration] MFDS 일괄 확정 인증·경합·롤백")
class AdminMfdsBulkMatchingConcurrencyIntegrationTest : IntegrationTestSupport() {
	@Autowired private lateinit var mfdsTestFactory: MfdsTestFactory

	@Autowired private lateinit var alcoholTestFactory: AlcoholTestFactory

	@Autowired private lateinit var bulkService: MfdsBulkMatchingService

	@Autowired private lateinit var jdbcTemplate: JdbcTemplate

	@MockitoBean private lateinit var lockGate: MfdsBulkLockGate

	@MockitoBean private lateinit var saveGuard: MfdsBulkSaveGuard

	private lateinit var accessToken: String
	private var adminId: Long = 0

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		adminId = admin.id
		accessToken = getAccessToken(admin)
	}

	@Test
	@DisplayName("인증이 없으면 일괄 미리보기를 거절한다")
	fun previewRequiresAuthentication() {
		val result = mockMvcTester.post().uri("/v1/mfds/matching/preview")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""{"sourceDeclarationId":1,"alcoholId":1}""")
			.exchange()
		assertThat(result).hasStatus(HttpStatus.FORBIDDEN)
	}

	@Test
	@DisplayName("인증이 없으면 일괄 확정을 거절한다")
	fun confirmRequiresAuthentication() {
		val result = mockMvcTester.post().uri("/v1/mfds/matching/confirm")
			.contentType(MediaType.APPLICATION_JSON)
			.content(confirmBody("a".repeat(64)))
			.exchange()
		assertThat(result).hasStatus(HttpStatus.FORBIDDEN)
	}

	@Test
	@DisplayName("일반 사용자 토큰은 일괄 미리보기와 확정을 거절한다")
	fun productUserTokenIsRejected() {
		val token = jwtTokenProvider.generateToken("member@example.com", UserType.ROLE_USER, 99L).accessToken()
		val preview = mockMvcTester.post().uri("/v1/mfds/matching/preview")
			.header("Authorization", "Bearer $token")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""{"sourceDeclarationId":1,"alcoholId":1}""")
			.exchange()
		val confirm = mockMvcTester.post().uri("/v1/mfds/matching/confirm")
			.header("Authorization", "Bearer $token")
			.contentType(MediaType.APPLICATION_JSON)
			.content(confirmBody("a".repeat(64)))
			.exchange()
		assertThat(preview).hasStatus(HttpStatus.FORBIDDEN)
		assertThat(confirm).hasStatus(HttpStatus.FORBIDDEN)
	}

	@Test
	@DisplayName("관리자 토큰은 인증을 통과하고 잘못된 발급 토큰은 권한 오류가 아니다")
	fun adminTokenPassesAuthentication() {
		val missingAlcohol = mockMvcTester.post().uri("/v1/mfds/matching/preview")
			.header("Authorization", "Bearer $accessToken")
			.contentType(MediaType.APPLICATION_JSON)
			.content("""{"sourceDeclarationId":1,"alcoholId":1}""")
			.exchange()
		val shortToken = mockMvcTester.post().uri("/v1/mfds/matching/confirm")
			.header("Authorization", "Bearer $accessToken")
			.contentType(MediaType.APPLICATION_JSON)
			.content(confirmBody("abc"))
			.exchange()
		val unknownToken = mockMvcTester.post().uri("/v1/mfds/matching/confirm")
			.header("Authorization", "Bearer $accessToken")
			.contentType(MediaType.APPLICATION_JSON)
			.content(confirmBody("a".repeat(64)))
			.exchange()
		assertThat(missingAlcohol).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(shortToken).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(unknownToken).hasStatus(HttpStatus.CONFLICT)
	}

	private fun confirmBody(token: String) = """
		{"sourceDeclarationId":1,"alcoholId":1,"declarationIds":[1],"previewToken":"$token","previewExpiresAt":"2099-01-01T00:00:00"}
	""".trimIndent()

	@Test
	@DisplayName("잠금 전에 단건 확정이 커밋되면 이전 미리보기로 덮어쓰지 않는다")
	fun concurrentSingleConfirmIsNotOverwritten() {
		val requested = alcoholTestFactory.persistAlcohol("요청 주류", "Requested", AlcoholType.WHISKY)
		val existing = alcoholTestFactory.persistAlcohol("기존 주류", "Existing", AlcoholType.WHISKY)
		val declaration = mfdsTestFactory.persistDeclaration("RCNO-RACE", MfdsNormalizationStatus.NORMALIZED, null, null, null)
		jdbcTemplate.update(
			"update mfds_declarations set product_identity_key_sha256 = ? where id = ?",
			key(1),
			declaration.id!!
		)
		val preview = bulkService.preview(MfdsBulkMatchingPreviewRequest(declaration.id!!, requested.id!!, null, null), adminId)
		val arrived = CountDownLatch(1)
		val changed = CountDownLatch(1)
		doAnswer {
			arrived.countDown()
			assertThat(changed.await(10, TimeUnit.SECONDS)).isTrue()
			null
		}.`when`(lockGate).beforeGroupLock(anyLong())
		val error = AtomicReference<Throwable>()
		val thread = Thread {
			try {
				bulkService.confirm(
					MfdsBulkMatchingConfirmRequest(
						declaration.id!!,
						requested.id!!,
						null,
						null,
						listOf(declaration.id!!),
						preview.previewToken(),
						preview.previewExpiresAt()
					),
					adminId
				)
			} catch (throwable: Throwable) {
				error.set(throwable)
			}
		}
		thread.start()
		assertThat(arrived.await(10, TimeUnit.SECONDS)).isTrue()
		jdbcTemplate.update(
			"update mfds_declarations set selected_alcohol_id = ? where id = ?",
			existing.id!!,
			declaration.id!!
		)
		changed.countDown()
		thread.join(10_000)
		assertThat(error.get()).isInstanceOf(MfdsException::class.java)
		assertThat(
			jdbcTemplate.queryForObject(
				"select selected_alcohol_id from mfds_declarations where id = ?",
				Long::class.java,
				declaration.id!!
			)
		).isEqualTo(existing.id)
		assertThat(
			jdbcTemplate.queryForObject(
				"select count(*) from mfds_matching_selections where declaration_id = ?",
				Long::class.java,
				declaration.id!!
			)
		).isZero()
	}

	@Test
	@DisplayName("두 번째 저장이 실패하면 본문과 감사 이력이 함께 롤백된다")
	fun secondSaveRollsBackEverything() {
		val alcohol = alcoholTestFactory.persistAlcohol("롤백 주류", "Rollback", AlcoholType.WHISKY)
		val first = mfdsTestFactory.persistDeclaration("RCNO-RB1", MfdsNormalizationStatus.NORMALIZED, null, null, null)
		val second = mfdsTestFactory.persistDeclaration("RCNO-RB2", MfdsNormalizationStatus.NORMALIZED, null, null, null)
		jdbcTemplate.update("update mfds_declarations set product_identity_key_sha256 = ? where id in (?, ?)", key(2), first.id!!, second.id!!)
		val preview = bulkService.preview(MfdsBulkMatchingPreviewRequest(first.id!!, alcohol.id!!, null, null), adminId)
		doThrow(IllegalStateException("nth save")).`when`(saveGuard).beforeSave(org.mockito.ArgumentMatchers.eq(1), anyLong())

		assertThatThrownBy {
			bulkService.confirm(
				MfdsBulkMatchingConfirmRequest(
					first.id!!,
					alcohol.id!!,
					null,
					null,
					listOf(first.id!!, second.id!!),
					preview.previewToken(),
					preview.previewExpiresAt()
				),
				adminId
			)
		}.isInstanceOf(IllegalStateException::class.java)

		assertThat(selected(first.id!!)).isNull()
		assertThat(selected(second.id!!)).isNull()
		assertThat(
			jdbcTemplate.queryForObject(
				"select count(*) from mfds_matching_selections where declaration_id in (?, ?)",
				Long::class.java,
				first.id!!,
				second.id!!
			)
		).isZero()
	}

	private fun selected(id: Long): Long? = jdbcTemplate.query(
		"select selected_alcohol_id from mfds_declarations where id = ?",
		{ rs, _ -> rs.getLong(1).takeUnless { rs.wasNull() } },
		id
	).single()

	private fun key(marker: Int) = ByteArray(32).also { it[31] = marker.toByte() }
}
