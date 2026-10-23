package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.constant.ScenarioTypes.*
import app.bottlenote.alcohols.constant.ScenarioTypes.Target
import app.bottlenote.alcohols.domain.*
import app.bottlenote.alcohols.domain.ScenarioDefinition.*
import app.bottlenote.alcohols.dto.request.AdminScenarioRequest.*
import app.bottlenote.alcohols.dto.request.ScenarioDefinitionRequest
import app.bottlenote.alcohols.exception.ScenarioException
import app.bottlenote.alcohols.fixture.AlcoholScenarioTestFactory.*
import app.bottlenote.alcohols.service.AdminAlcoholScenarioService
import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Tag("admin_integration")
class AdminAlcoholScenarioPersistenceIntegrationTest : IntegrationTestSupport() {
	@Autowired private lateinit var jdbc: JdbcTemplate

	@Autowired private lateinit var service: AdminAlcoholScenarioService

	@Autowired private lateinit var questions: AlcoholScenarioQuestionRepository

	@Autowired private lateinit var emf: EntityManagerFactory

	@Autowired private lateinit var repository: AlcoholScenarioRepository

	@Autowired private lateinit var transactions: PlatformTransactionManager
	private val groupId = 730001L

	private fun request(d: ScenarioDefinition): ScenarioDefinitionRequest = mapper.convertValue(d, ScenarioDefinitionRequest::class.java)

	@Test
	@DisplayName("최대 초안을 저장하고 순서와 참조를 바꿀 때 ID와 자식을 보존하며 flush 횟수를 제한한다")
	fun savesMaximumGraphAndReordersWithoutPerEntityFlushes() {
		jdbc.update("INSERT INTO alcohol_attribute_groups(id,name) VALUES(?,?)", groupId, "최대 그래프")
		val scenario = service.create(groupId, Create("최대 그래프", 1))
		val id = scenario.scenario().id()
		val versionId = scenario.versions().first().id()
		val stages = (1..50).map { ScenarioStageItem(-it.toLong(), it) }
		val questions = (1..100).map { choice(-it.toLong(), -((it + 1) / 2).toLong(), (it - 1) % 2 + 1, "질문 $it", Source.CATEGORY, null, "SINGLE_MALT", "BOURBON", "BLEND", "OTHER") }
		val transitions = questions.flatMapIndexed { index, q ->
			(1..5).map { priority -> rule(-(index * 5L + priority), q.id(), priority, if (priority == 5) Operator.ALWAYS else Operator.EQ, if (priority == 5) null else q.inputConfig().choices()[priority - 1].id(), null) }
		}
		val definition = ScenarioDefinition(1, -1, stages, questions, transitions)
		val statistics = emf.unwrap(SessionFactory::class.java).statistics
		val previous = statistics.isStatisticsEnabled
		statistics.isStatisticsEnabled = true
		try {
			statistics.clear()
			val saved = service.save(groupId, id, versionId, Save(0, request(definition)))
			assertThat(saved.definition().stages()).hasSize(50)
			assertThat(saved.definition().questions()).hasSize(100)
			assertThat(saved.definition().transitions()).hasSize(500)
			assertThat(statistics.flushCount).isLessThanOrEqualTo(6)
			assertThat(statistics.prepareStatementCount).isGreaterThanOrEqualTo(650)
			println("maximum insert: flush=${statistics.flushCount}, preparedStatements=${statistics.prepareStatementCount}")
			val d = mapper.convertValue(saved.definition(), ScenarioDefinition::class.java)
			val reordered = ScenarioDefinition(1, d.startQuestionId(), d.stages().map { ScenarioStageItem(it.id(), 51 - it.position()) }, d.questions().map { ScenarioQuestionItem(it.id(), it.stageId(), 3 - it.branchOrder(), it.title(), it.sourceKind(), it.attributeId(), it.inputKind(), it.inputConfig()) }, d.transitions().map { ScenarioTransitionItem(it.id(), it.fromQuestionId(), 6 - it.priority(), it.predicate(), it.targetKind(), it.targetQuestionId()) })
			statistics.clear()
			val updated = service.save(groupId, id, versionId, Save(1, request(reordered)))
			assertThat(updated.definition().questions().map { it.id() }).containsExactlyInAnyOrderElementsOf(d.questions().map { it.id() })
			assertThat(updated.definition().transitions().map { it.id() }).containsExactlyInAnyOrderElementsOf(d.transitions().map { it.id() })
			assertThat(statistics.flushCount).isLessThanOrEqualTo(6)
			println("maximum reorder: flush=${statistics.flushCount}, preparedStatements=${statistics.prepareStatementCount}")
			val q = d.questions().first()
			val stage = d.stages()[1]
			val t = d.transitions().first()
			val pruned = ScenarioDefinition(1, q.id(), listOf(stage), listOf(ScenarioQuestionItem(q.id(), stage.id(), 1, q.title(), q.sourceKind(), null, q.inputKind(), q.inputConfig())), listOf(ScenarioTransitionItem(t.id(), q.id(), 1, ScenarioPredicate(Operator.ALWAYS, null), Target.END, null)))
			val result = service.save(groupId, id, versionId, Save(2, request(pruned)))
			assertThat(result.definition().questions().single().id()).isEqualTo(q.id())
			assertThat(result.definition().questions().single().stageId()).isEqualTo(stage.id())
			assertThat(result.definition().transitions().single().id()).isEqualTo(t.id())
			assertThat(requireNotNull(jdbc.queryForObject("SELECT COUNT(*) FROM alcohol_attribute_scenario_questions WHERE version_id=?", Long::class.java, versionId))).isEqualTo(1)
		} finally {
			statistics.isStatisticsEnabled = previous
		}
	}

	@Test
	@DisplayName("게시 맛 그룹 참조를 여러 코드로 검사할 때 JSON 값을 정확히 비교하고 엔티티 없이 한 쿼리로 조회한다")
	fun checksPublishedTastingCodesInOneExistenceQuery() {
		jdbc.update("INSERT INTO alcohol_attribute_groups(id,name) VALUES(?,?)", groupId, "참조 검사")
		jdbc.update("INSERT INTO tasting_tag_groups(code,name,display_order) VALUES('PEAT','피트',0),('SWEET','단맛',1)")
		fun create(slot: Int, code: String, publish: Boolean) {
			val s = service.create(groupId, Create("참조 검사", slot))
			val base = oneQuestion()
			val d = ScenarioDefinition(1, -11, base.stages(), listOf(choice(-11, -1, 1, "맛", Source.TASTING_GROUP, null, code)), base.transitions())
			service.save(groupId, s.scenario().id(), s.versions().first().id(), Save(0, request(d)))
			if (publish) service.publish(groupId, s.scenario().id(), s.versions().first().id(), Publish(0, 1))
		}
		create(1, "PEAT", true)
		create(2, "SWEET", false)
		assertThat(questions.hasPublishedTastingGroupReferences(emptyList())).isFalse()
		assertThat(questions.hasPublishedTastingGroupReferences(listOf("SWEET", "MISSING"))).isFalse()
		assertThat(questions.hasPublishedTastingGroupReferences(listOf("peat", "PEAT_X"))).isFalse()
		val statistics = emf.unwrap(SessionFactory::class.java).statistics
		val previous = statistics.isStatisticsEnabled
		statistics.isStatisticsEnabled = true
		try {
			statistics.clear()
			assertThat(questions.hasPublishedTastingGroupReferences(listOf("MISSING", "PEAT", "SWEET"))).isTrue()
			assertThat(statistics.entityLoadCount).isZero()
			assertThat(statistics.prepareStatementCount).isEqualTo(1)
		} finally {
			statistics.isStatisticsEnabled = previous
		}
	}

	@Test
	@DisplayName("서비스 슬롯 사전 검사 밖에서도 DB 고유 키 충돌을 시나리오 충돌 오류로 변환한다")
	fun translatesUniqueSlotCollisionAtRepositoryBoundary() {
		jdbc.update("INSERT INTO alcohol_attribute_groups(id,name) VALUES(?,?)", groupId, "슬롯 고유 키")
		service.create(groupId, Create("첫 시나리오", 1))
		val rejected = runCatching {
			TransactionTemplate(transactions).execute { repository.saveScenario(AlcoholAttributeScenario.create(groupId, 1, "중복 시나리오")) }
		}.exceptionOrNull() as ScenarioException
		assertThat(rejected.exceptionCode.httpStatus.value()).isEqualTo(409)
		assertThat(repository.findScenarios(groupId)).hasSize(1)
	}
}
