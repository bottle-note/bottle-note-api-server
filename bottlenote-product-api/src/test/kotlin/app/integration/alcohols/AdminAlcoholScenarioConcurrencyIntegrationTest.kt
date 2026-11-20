package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.constant.ScenarioTypes.Target
import app.bottlenote.alcohols.domain.*
import app.bottlenote.alcohols.domain.ScenarioDefinition
import app.bottlenote.alcohols.domain.ScenarioDefinition.ScenarioTransitionItem
import app.bottlenote.alcohols.dto.request.AdminScenarioRequest.*
import app.bottlenote.alcohols.exception.ScenarioException
import app.bottlenote.alcohols.fixture.AlcoholScenarioTestFactory
import app.bottlenote.alcohols.service.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Tag("admin_integration")
class AdminAlcoholScenarioConcurrencyIntegrationTest : IntegrationTestSupport() {
	@Autowired private lateinit var jdbc: JdbcTemplate

	@Autowired private lateinit var service: AdminAlcoholScenarioService

	@Autowired private lateinit var repository: AlcoholScenarioRepository

	@Autowired private lateinit var versions: AlcoholScenarioVersionRepository

	@Autowired private lateinit var stages: AlcoholScenarioStageRepository

	@Autowired private lateinit var questions: AlcoholScenarioQuestionRepository

	@Autowired private lateinit var transitions: AlcoholScenarioTransitionRepository

	@Autowired private lateinit var groups: AlcoholAttributeGroupRepository

	@Autowired private lateinit var attributes: AlcoholAttributeRepository

	@Autowired private lateinit var tastingGroups: TastingTagGroupRepository

	@Autowired private lateinit var validator: ScenarioDefinitionValidator

	@Autowired private lateinit var preview: ScenarioPreviewService

	@Autowired private lateinit var transactions: PlatformTransactionManager
	private val groupId = 720001L

	@Test
	@DisplayName("그래프 조회 후 저장이 커밋될 때 정상 revision 게시를 409로 거절한다")
	fun rejectsCurrentRevisionWhenSaveCommitsAfterGraphRead() = race(1)

	@Test
	@DisplayName("미래 revision 게시를 요청할 때 이전 그래프 검증을 시작하기 전에 409로 거절한다")
	fun rejectsPredictedRevisionWhenSaveCommitsAfterGraphRead() = race(2)

	@Test
	@DisplayName("그래프 SELECT 사이에 단계가 교체될 때 NPE 없이 409로 거절한다")
	fun rejectsRevisionWhenSaveReplacesStagesBetweenGraphSelects() = race(1, true)

	private fun race(expected: Long, betweenSelects: Boolean = false) {
		jdbc.update("INSERT INTO alcohol_attribute_groups(id,name) VALUES(?,?)", groupId, "게시 경합")
		val created = service.create(groupId, Create("게시 경합", 1))
		val id = created.scenario().id()
		val versionId = created.versions().first().id()
		val saved = service.save(groupId, id, versionId, Save(0, mapper.convertValue(AlcoholScenarioTestFactory.oneQuestion(), ScenarioDefinition::class.java)))
		val graphRead = CompletableFuture<Unit>()
		val resume = CountDownLatch(1)
		val controlledStages = object : AlcoholScenarioStageRepository by stages {
			override fun findStages(id: Long?): List<AlcoholAttributeScenarioStage> {
				val result = stages.findStages(id)
				if (betweenSelects) {
					graphRead.complete(Unit)
					check(resume.await(10, TimeUnit.SECONDS))
				}
				return result
			}
		}
		val controlledTransitions = object : AlcoholScenarioTransitionRepository by transitions {
			override fun findTransitions(id: Long?): List<AlcoholAttributeScenarioTransition> {
				val result = transitions.findTransitions(id)
				if (!betweenSelects) {
					graphRead.complete(Unit)
					check(resume.await(10, TimeUnit.SECONDS))
				}
				return result
			}
		}
		val publisher = DefaultAdminAlcoholScenarioService(repository, versions, ScenarioDefinitionStore(repository, controlledStages, questions, controlledTransitions), groups, attributes, tastingGroups, validator, preview)
		val tx = TransactionTemplate(transactions).apply { isolationLevel = TransactionDefinition.ISOLATION_READ_COMMITTED }
		val publish = CompletableFuture.supplyAsync {
			try {
				tx.execute { publisher.publish(groupId, id, versionId, Publish(0, expected)) }
				200
			} catch (ex: ScenarioException) {
				ex.exceptionCode.httpStatus.value()
			}
		}
		try {
			CompletableFuture.anyOf(graphRead, publish).get(10, TimeUnit.SECONDS)
			val d = mapper.convertValue(saved.definition(), ScenarioDefinition::class.java)
			val t = d.transitions().first()
			val invalid = ScenarioDefinition(d.definitionSchemaVersion(), d.startQuestionId(), d.stages(), d.questions(), listOf(ScenarioTransitionItem(t.id(), t.fromQuestionId(), t.priority(), t.predicate(), Target.QUESTION, t.fromQuestionId())))
			val replacement = if (!betweenSelects) {
				invalid
			} else {
				val fresh = AlcoholScenarioTestFactory.oneQuestion()
				val q = fresh.questions().first()
				ScenarioDefinition(1, -11, fresh.stages(), listOf(q, ScenarioDefinition.ScenarioQuestionItem(-12, -1, 2, "새 질문", q.sourceKind(), null, q.inputKind(), q.inputConfig())), listOf(AlcoholScenarioTestFactory.rule(-101, -11, 1, app.bottlenote.alcohols.constant.ScenarioTypes.Operator.ALWAYS, null, -12L), AlcoholScenarioTestFactory.rule(-102, -12, 1, app.bottlenote.alcohols.constant.ScenarioTypes.Operator.ALWAYS, null, null)))
			}
			service.save(groupId, id, versionId, Save(1, mapper.convertValue(replacement, ScenarioDefinition::class.java)))
		} finally {
			resume.countDown()
		}
		assertThat(publish.get(10, TimeUnit.SECONDS)).isEqualTo(409)
		assertThat(graphRead.isDone).isEqualTo(expected == 1L)
		assertThat(service.getVersion(groupId, id, versionId).version().status().name).isEqualTo("DRAFT")
		assertThat(requireNotNull(jdbc.queryForObject("SELECT COUNT(*) FROM alcohol_attribute_scenarios WHERE id=? AND active_version_id IS NULL", Long::class.java, id))).isEqualTo(1)
		val rejected = runCatching { service.publish(groupId, id, versionId, Publish(0, 2)) }.exceptionOrNull() as ScenarioException
		assertThat(rejected.exceptionCode.httpStatus.value()).isEqualTo(400)
	}
}
