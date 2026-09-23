package app.integration.mfds

import app.IntegrationTestSupport
import app.bottlenote.alcohols.constant.AlcoholType
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.mfds.constant.MfdsNormalizationStatus
import app.bottlenote.mfds.domain.MfdsDeclarationRepository
import app.bottlenote.mfds.domain.MfdsMatchingRepository
import app.bottlenote.mfds.dto.request.MfdsMatchingConfirmRequest
import app.bottlenote.mfds.fixture.MfdsTestData
import app.bottlenote.mfds.fixture.MfdsTestFactory
import app.bottlenote.mfds.repository.JpaMfdsMatchingSelectionRepository
import app.bottlenote.mfds.service.MfdsMatchingService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Tag("admin_integration")
@DisplayName("[integration] Admin MFDS 확정과 감사 기록 통합 테스트")
class AdminMfdsMatchingIntegrationTest : IntegrationTestSupport() {
	@Autowired
	private lateinit var mfdsTestFactory: MfdsTestFactory

	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	@Autowired
	private lateinit var declarationRepository: MfdsDeclarationRepository

	@Autowired
	private lateinit var selectionRepository: JpaMfdsMatchingSelectionRepository

	@Autowired
	private lateinit var matchingService: MfdsMatchingService

	@Autowired
	private lateinit var transactionManager: PlatformTransactionManager

	@Autowired
	private lateinit var jdbcTemplate: JdbcTemplate

	@Autowired
	private lateinit var matchingRepository: MfdsMatchingRepository

	private lateinit var accessToken: String
	private var adminId: Long = 0

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		adminId = admin.id
		accessToken = getAccessToken(admin)
	}

	@Test
	@DisplayName("매칭을 실행할 때 위스키 상위 10개를 DB에 저장하고 다시 조회한다")
	fun runPersistsAndReturnsTenAlcoholCandidates() {
		repeat(12) {
			alcoholTestFactory.persistAlcohol("글렌피딕 12", "Glenfiddich 12", AlcoholType.WHISKY)
		}
		val declaration = mfdsTestFactory.persistDeclaration("RCNO-TOP10", MfdsNormalizationStatus.NORMALIZED, null, null, null)
		MfdsTestData.set(declaration, "nameSearchKeyKo", "글렌피딕 12")
		MfdsTestData.set(declaration, "nameSearchKeyEn", "Glenfiddich 12")
		declarationRepository.save(declaration)

		val run = mockMvcTester.post().uri("/v1/mfds/declarations/${declaration.id}/matching/run")
			.header("Authorization", "Bearer $accessToken").exchange()
		assertThat(run).hasStatusOk()
		val ranked = mapper.readTree(run.response.contentAsString).path("data").path("alcoholCandidates")
		assertThat(ranked.size()).isEqualTo(10)
		val expectedIds = ranked.map { it.path("alcoholId").asLong() }
		val stored = declarationRepository.findById(declaration.id).orElseThrow()
		assertThat(matchingRepository.findCandidates(stored.matchingRunId, stored.id).filter { it.targetType == "ALCOHOL" }.map { it.targetId }).containsExactlyElementsOf(expectedIds)
		assertThat(matchingRepository.findCandidates(stored.matchingRunId, stored.id).filter { it.targetType == "ALCOHOL" }.map { it.rawScore.toDouble() })
			.containsExactlyElementsOf(ranked.map { it.path("score").asDouble() })

		val fetched = mockMvcTester.get().uri("/v1/mfds/declarations/${declaration.id}/matching/candidates")
			.header("Authorization", "Bearer $accessToken").exchange()
		assertThat(fetched).hasStatusOk()
		assertThat(mapper.readTree(fetched.response.contentAsString).path("data").path("alcoholCandidates").map { it.path("alcoholId").asLong() })
			.containsExactlyElementsOf(expectedIds)

		assertThat(confirm(declaration.id, expectedIds.last())).hasStatusOk()
		assertThat(declarationRepository.findById(declaration.id).orElseThrow().alcoholMatchDecision).isEqualTo("CANDIDATE")
	}

	@Test
	@DisplayName("마이그레이션 후 고정 후보 컬럼은 제거하고 확정값과 상세 뷰는 유지한다")
	fun removesLegacyCandidateColumnsAndKeepsDetailView() {
		val columns = jdbcTemplate.queryForList("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mfds_declarations'", String::class.java)
		val legacyColumns = listOf("alcohol", "distillery", "region").flatMap { type ->
			(1..3).flatMap { rank -> listOf("${type}_candidate_${rank}_id", "${type}_candidate_${rank}_score") }
		}
		assertThat(columns).doesNotContainAnyElementsOf(legacyColumns)
		assertThat(columns).contains("selected_alcohol_id", "selected_distillery_id", "selected_region_id", "matching_run_id", "cask_candidate", "distillery_name_ko_candidate")
		val sourceItemId = mfdsTestFactory.persistItem("RCNO-SCHEMA", "테스트 주류", java.time.LocalDate.of(2026, 9, 24), java.time.LocalDateTime.of(2026, 9, 24, 0, 0))
		val declaration = mfdsTestFactory.persistDeclaration("RCNO-SCHEMA", MfdsNormalizationStatus.NORMALIZED, null, null, null)
		jdbcTemplate.update("UPDATE mfds_declarations SET source_item_id = ?, selected_alcohol_id = 123, selected_distillery_id = 456, selected_region_id = 789 WHERE id = ?", sourceItemId, declaration.id)
		val detail = jdbcTemplate.queryForMap("SELECT * FROM mfds_declaration_details WHERE id = ?", declaration.id)
		assertThat(detail.keys).doesNotContainAnyElementsOf(legacyColumns)
		assertThat((detail["selected_alcohol_id"] as Number).toLong()).isEqualTo(123L)
		assertThat((detail["selected_distillery_id"] as Number).toLong()).isEqualTo(456L)
		assertThat((detail["selected_region_id"] as Number).toLong()).isEqualTo(789L)
		assertThat(jdbcTemplate.queryForObject("SELECT TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'mfds_matching_candidates'", String::class.java)).contains("실행", "규칙 버전").doesNotContain("상위 3개")
	}

	@Test
	@DisplayName("고정 후보 컬럼 없이 실행 테이블로 조회하고 재계산은 이전 이력을 보존한다")
	fun readsRunCandidatesAndPreservesHistory() {
		alcoholTestFactory.persistAlcohol("글렌모렌지 오리지널 12년", "Glenmorangie Original 12yo", AlcoholType.WHISKY)
		val d = mfdsTestFactory.persistDeclaration("RCNO-HISTORY", MfdsNormalizationStatus.NORMALIZED, null, null, null)
		jdbcTemplate.update("UPDATE mfds_declarations SET name_search_key_en = 'Glenmorangie Original 12yo' WHERE id = ?", d.id)
		assertThat(matchingService.getCandidates(d.id).alcoholCandidates()).isEmpty()
		assertThat(matchingService.getCandidates(d.id).distilleryCandidates()).isEmpty()
		val first = matchingService.runMatching(d.id)
		val firstRun = declarationRepository.findById(d.id).orElseThrow().matchingRunId
		assertThat(first.alcoholCandidates()).hasSize(1)
		assertThat(matchingService.getCandidates(d.id).alcoholCandidates().first().scoreDetail()).isEqualTo(first.alcoholCandidates().first().scoreDetail())
		val detail = mockMvcTester.get().uri("/v1/mfds/declarations/${d.id}").header("Authorization", "Bearer $accessToken").exchange()
		assertThat(detail).hasStatusOk()
		assertThat(mapper.readTree(detail.response.contentAsString).path("data").path("alcoholCandidates").size()).isEqualTo(1)
		jdbcTemplate.update("UPDATE mfds_declarations SET name_search_key_en = 'UnrelatedBrand UnknownProduct' WHERE id = ?", d.id)
		matchingService.runMatching(d.id)
		val latest = declarationRepository.findById(d.id).orElseThrow()
		assertThat(latest.matchingRunId).isNotEqualTo(firstRun)
		assertThat(matchingService.getCandidates(d.id).alcoholCandidates()).isEmpty()
		assertThat(matchingRepository.findCandidates(firstRun, d.id)).hasSize(1)
	}

	@Test
	@DisplayName("매칭 트랜잭션을 롤백할 때 실행과 후보 및 최신 실행 연결을 함께 되돌린다")
	fun rollsBackRunCandidatesAndPointerTogether() {
		alcoholTestFactory.persistAlcohol("글렌피딕", "Glenfiddich", AlcoholType.WHISKY)
		val d = mfdsTestFactory.persistDeclaration("RCNO-ROLLBACK", MfdsNormalizationStatus.NORMALIZED, null, null, null)
		jdbcTemplate.update("UPDATE mfds_declarations SET name_search_key_en = 'Glenfiddich' WHERE id = ?", d.id)
		val before = requireNotNull(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mfds_matching_runs", Long::class.java))
		org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult { status ->
			matchingService.runMatching(d.id)
			status.setRollbackOnly()
		}
		assertThat(requireNotNull(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mfds_matching_runs", Long::class.java))).isEqualTo(before)
		assertThat(requireNotNull(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mfds_matching_candidates WHERE declaration_id = ?", Long::class.java, d.id))).isZero()
		assertThat(jdbcTemplate.queryForMap("SELECT matching_run_id FROM mfds_declarations WHERE id = ?", d.id)["matching_run_id"]).isNull()
	}

	@Test
	@DisplayName("주류만 반복 확정할 때 증류소·지역을 전파하고 관리자 이력을 누적한다")
	fun confirmPropagatesReferencesAndAppendsAudit() {
		val alcohol = alcoholTestFactory.persistAlcohol()
		val declaration = mfdsTestFactory.persistDeclaration("RCNO-AUDIT", MfdsNormalizationStatus.NORMALIZED, null, null, null)

		repeat(2) {
			val result = confirm(declaration.id, alcohol.id)
			assertThat(result).hasStatusOk()
			val data = mapper.readTree(result.response.contentAsString).path("data")
			assertThat(data.path("selectedDistilleryId").asLong()).isEqualTo(alcohol.distillery.id)
			assertThat(data.path("selectedRegionId").asLong()).isEqualTo(alcohol.region.id)
			assertThat(data.path("distilleryMatchSource").asText()).isEqualTo("ALCOHOL_PROPAGATED")
			assertThat(data.path("regionMatchSource").asText()).isEqualTo("ALCOHOL_PROPAGATED")
		}

		val selections = selectionRepository.findAll().sortedBy { it.id }
		assertThat(selections).hasSize(6)
		assertThat(selections.map { it.targetType }).containsExactly("ALCOHOL", "DISTILLERY", "REGION", "ALCOHOL", "DISTILLERY", "REGION")
		assertThat(selections.map { it.targetId }).containsExactly(alcohol.id, alcohol.distillery.id, alcohol.region.id, alcohol.id, alcohol.distillery.id, alcohol.region.id)
		assertThat(selections.map { it.reasonCode }).containsExactly("MANUAL", "ALCOHOL_PROPAGATED", "ALCOHOL_PROPAGATED", "MANUAL", "ALCOHOL_PROPAGATED", "ALCOHOL_PROPAGATED")
		selections.forEach {
			assertThat(it.runId as Any?).isNull()
			assertThat(it.declarationId).isEqualTo(declaration.id)
			assertThat(it.action).isEqualTo("SELECT")
			assertThat(it.selectionSource).isEqualTo("ADMIN")
			assertThat(it.selectedBy).isEqualTo(adminId.toString())
			assertThat(it.selectedAt).isNotNull()
		}
	}

	@Test
	@DisplayName("상속된 확정을 해제할 때 선택과 상속 원본을 비우고 대상별 REVOKE를 기록한다")
	fun releaseClearsInheritedSelectionAndAuditsPreviousTargets() {
		val declaration = mfdsTestFactory.persistDeclaration("RCNO-RELEASE", MfdsNormalizationStatus.NORMALIZED, null, 101L, "INHERITED")
		MfdsTestData.set(declaration, "selectedDistilleryId", 102L)
		MfdsTestData.set(declaration, "distilleryMatchSource", "INHERITED")
		MfdsTestData.set(declaration, "selectedRegionId", 103L)
		MfdsTestData.set(declaration, "regionMatchSource", "INHERITED")
		MfdsTestData.set(declaration, "inheritedFromDeclarationId", 100L)
		declarationRepository.save(declaration)

		assertThat(
			mockMvcTester.post().uri("/v1/mfds/declarations/${declaration.id}/matching/release")
				.header("Authorization", "Bearer $accessToken")
		).hasStatusOk()

		val released = declarationRepository.findById(declaration.id).orElseThrow()
		assertThat(released.selectedAlcoholId as Any?).isNull()
		assertThat(released.alcoholMatchDecision).isNull()
		assertThat(released.selectedDistilleryId as Any?).isNull()
		assertThat(released.distilleryMatchSource).isNull()
		assertThat(released.selectedRegionId as Any?).isNull()
		assertThat(released.regionMatchSource).isNull()
		assertThat(released.inheritedFromDeclarationId as Any?).isNull()
		val selections = selectionRepository.findAll().sortedBy { it.id }
		assertThat(selections.map { it.targetType }).containsExactly("ALCOHOL", "DISTILLERY", "REGION")
		assertThat(selections.map { it.targetId }).containsExactly(101L, 102L, 103L)
		selections.forEach {
			assertThat(it.action).isEqualTo("REVOKE")
			assertThat(it.reasonCode).isEqualTo("ADMIN_RELEASE")
			assertThat(it.selectedBy).isEqualTo(adminId.toString())
			assertThat(it.selectionSource).isEqualTo("ADMIN")
		}
	}

	@Test
	@DisplayName("상속 판정으로 목록·상세·후보를 조회하고 확정할 때 상속 원본만 비운다")
	fun inheritedCanBeFilteredReadAndConfirmed() {
		val alcohol = alcoholTestFactory.persistAlcohol()
		val declaration = mfdsTestFactory.persistDeclaration("RCNO-INHERITED", MfdsNormalizationStatus.NORMALIZED, null, alcohol.id, "INHERITED")
		mfdsTestFactory.persistDeclaration("RCNO-OTHER", MfdsNormalizationStatus.NORMALIZED, null, null, "MANUAL")
		MfdsTestData.set(declaration, "inheritedFromDeclarationId", 100L)
		declarationRepository.save(declaration)
		val identityKey = ByteArray(32) { 1 }
		jdbcTemplate.update("UPDATE mfds_declarations SET product_identity_key_sha256 = ? WHERE id = ?", identityKey, declaration.id)

		val list = mockMvcTester.get().uri("/v1/mfds/declarations?alcoholMatchDecision=INHERITED")
			.header("Authorization", "Bearer $accessToken").exchange()
		assertThat(list).hasStatusOk().bodyJson().extractingPath("$.data.length()").isEqualTo(1)
		assertThat(list).bodyJson().extractingPath("$.data[0].alcoholMatchDecision").isEqualTo("INHERITED")
		assertThat(
			mockMvcTester.get().uri("/v1/mfds/declarations/${declaration.id}")
				.header("Authorization", "Bearer $accessToken")
		).hasStatusOk()
			.bodyJson().extractingPath("$.data.alcoholMatchDecision").isEqualTo("INHERITED")
		assertThat(
			mockMvcTester.get().uri("/v1/mfds/declarations/${declaration.id}/matching/candidates")
				.header("Authorization", "Bearer $accessToken")
		).hasStatusOk()
			.bodyJson().extractingPath("$.data.selection.alcoholMatchDecision").isEqualTo("INHERITED")

		assertThat(confirm(declaration.id, alcohol.id)).hasStatusOk()
			.bodyJson().extractingPath("$.data.alcoholMatchDecision").isEqualTo("MANUAL")
		assertThat(declarationRepository.findById(declaration.id).orElseThrow().inheritedFromDeclarationId as Any?).isNull()
		assertThat(jdbcTemplate.queryForObject("SELECT product_identity_key_sha256 FROM mfds_declarations WHERE id = ?", ByteArray::class.java, declaration.id)).isEqualTo(identityKey)
	}

	@Test
	@DisplayName("삭제된 주류로 확정할 때 선택과 감사 이력을 변경하지 않는다")
	fun deletedAlcoholDoesNotCreateAudit() {
		val alcohol = alcoholTestFactory.persistAlcohol()
		jdbcTemplate.update("UPDATE alcohols SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?", alcohol.id)
		val declaration = mfdsTestFactory.persistDeclaration("RCNO-DELETED", MfdsNormalizationStatus.NORMALIZED, null, null, null)

		assertThat(confirm(declaration.id, alcohol.id)).hasStatus(400)
		assertThat(selectionRepository.count()).isZero()
		assertThat(declarationRepository.findById(declaration.id).orElseThrow().selectedAlcoholId as Any?).isNull()
	}

	@Test
	@DisplayName("인증 없이 확정을 요청할 때 선택과 감사 이력을 변경하지 않는다")
	fun unauthenticatedCannotConfirm() {
		val alcohol = alcoholTestFactory.persistAlcohol()
		val declaration = mfdsTestFactory.persistDeclaration("RCNO-UNAUTHORIZED", MfdsNormalizationStatus.NORMALIZED, null, null, null)

		assertThat(
			mockMvcTester.post().uri("/v1/mfds/declarations/${declaration.id}/matching/confirm")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(MfdsMatchingConfirmRequest(alcohol.id, null, null)))
		).hasStatus(403)
		assertThat(selectionRepository.count()).isZero()
		assertThat(declarationRepository.findById(declaration.id).orElseThrow().selectedAlcoholId as Any?).isNull()
	}

	@Test
	@DisplayName("확정 트랜잭션을 롤백할 때 선택 변경과 감사 이력을 함께 되돌린다")
	fun selectionAndAuditRollBackTogether() {
		val alcohol = alcoholTestFactory.persistAlcohol()
		val declaration = mfdsTestFactory.persistDeclaration("RCNO-ROLLBACK", MfdsNormalizationStatus.NORMALIZED, null, null, null)

		assertThatThrownBy {
			TransactionTemplate(transactionManager).executeWithoutResult {
				matchingService.confirmMatching(declaration.id, MfdsMatchingConfirmRequest(alcohol.id, null, null), adminId)
				assertThat(selectionRepository.count()).isEqualTo(3)
				throw IllegalStateException("rollback test")
			}
		}.isInstanceOf(IllegalStateException::class.java)

		assertThat(selectionRepository.count()).isZero()
		assertThat(declarationRepository.findById(declaration.id).orElseThrow().selectedAlcoholId as Any?).isNull()
	}

	private fun confirm(declarationId: Long, alcoholId: Long) = mockMvcTester.post()
		.uri("/v1/mfds/declarations/$declarationId/matching/confirm")
		.header("Authorization", "Bearer $accessToken")
		.contentType(MediaType.APPLICATION_JSON)
		.content(mapper.writeValueAsString(MfdsMatchingConfirmRequest(alcoholId, null, null)))
		.exchange()
}
