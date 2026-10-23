package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.fixture.AlcoholScenarioTestFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.assertj.MvcTestResult
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Tag("admin_integration")
@DisplayName("시나리오를 관리할 때 저장·게시·분기·동시성을 검증한다")
class AdminAlcoholScenarioIntegrationTest : IntegrationTestSupport() {
	@Autowired private lateinit var jdbc: JdbcTemplate
	private val group = 710001L
	private val experience = 710001L
	private val price = 710002L
	private fun scalar(sql: String, vararg values: Any): Long = requireNotNull(jdbc.queryForObject(sql, Long::class.java, *values))

	private val base = "/v1/attribute/groups/$group/scenarios"

	private fun seed(): String {
		val token = getAccessToken(adminUserTestFactory.persistRootAdmin())
		jdbc.update("INSERT INTO alcohol_attribute_groups(id,name) VALUES(?,?)", group, "시나리오 테스트")
		jdbc.update("INSERT INTO alcohol_attribute_groups(id,name) VALUES(?,?)", group + 1, "다른 시나리오 그룹")
		jdbc.update("INSERT INTO alcohol_attributes(id,group_id,attribute_key,display_name,attribute_type,datatype,allow_multiple_values) VALUES(?,?,'scenario_experience','시나리오 경험','KEYWORD','STRING',true)", experience, group)
		jdbc.update("INSERT INTO alcohol_attributes(id,group_id,attribute_key,display_name,attribute_type,datatype,allow_multiple_values) VALUES(?,?,'scenario_price','시나리오 가격','PRICE','INT',true)", price, group)
		jdbc.update("INSERT INTO tasting_tag_groups(id,code,name,display_order) VALUES(910001,'PEAT','피트',0),(910002,'SWEET','달콤함',1)")
		jdbc.update("INSERT INTO tasting_tags(id,kor_name,eng_name) VALUES(910001,'피트 태그','Peat'),(910002,'단맛 태그','Sweet')")
		jdbc.update("INSERT INTO tasting_tag_group_tags(group_id,tasting_tag_id,role) VALUES(910001,910001,'CORE'),(910002,910002,'CORE')")
		for (i in 1..3) {
			jdbc.update("INSERT INTO alcohols(id,kor_name,eng_name,type,category_group,abv,kor_category,eng_category,age,cask,volume) VALUES(?,?,?,'WHISKY',?,?,'싱글몰트','Single Malt','12','BOURBON','700')", 810000L + i, "시나리오 후보 $i", "Scenario $i", if (i == 3) "BOURBON" else "SINGLE_MALT", if (i == 3) "50%" else "40%")
			jdbc.update("INSERT INTO alcohol_attribute_mappings(alcohol_id,attribute_id,attribute_value) VALUES(?,?,?)", 810000L + i, price, (i * 10000).toString())
			for (value in listOf("newbie", "couple", "tried", "expert")) {
				jdbc.update("INSERT INTO alcohol_attribute_mappings(alcohol_id,attribute_id,attribute_value) VALUES(?,?,?)", 810000L + i, experience, value)
			}
		}
		jdbc.update("INSERT INTO alcohols_tasting_tags(alcohol_id,tasting_tag_id) VALUES(810001,910001),(810002,910001),(810002,910002)")
		return token
	}
	private fun data(result: MvcTestResult): JsonNode {
		assertThat(result).hasStatusOk()
		return mapper.readTree(result.response.contentAsString)["data"]
	}
	private fun create(token: String, slot: Int = 1) = data(postAs(token, base, mapOf("slot" to slot, "title" to "추천 시나리오")))
	private fun scenarioUrl(s: JsonNode) = "$base/${s.at("/scenario/id").asLong()}"
	private fun versionUrl(s: JsonNode) = "${scenarioUrl(s)}/versions/${s.at("/versions/0/id").asLong()}"
	private fun definition() = mapper.valueToTree<ObjectNode>(AlcoholScenarioTestFactory.sevenStages(experience, price))
	private fun oneQuestion() = mapper.valueToTree<ObjectNode>(AlcoholScenarioTestFactory.oneQuestion())
	private fun save(token: String, url: String, definition: JsonNode, revision: Long = 0) = data(putAs(token, url, mapOf("expectedLockVersion" to revision, "definition" to definition)))
	private fun publish(token: String, url: String, revision: Long = 1, scenarioRevision: Long = 0) = postAs(token, "$url/publish", mapOf("expectedLockVersion" to revision, "expectedScenarioLockVersion" to scenarioRevision))
	private fun question(d: JsonNode, name: String) = d.at("/definition/questions").first { it["title"].asText() == name }["id"].asLong()
	private fun choice(d: JsonNode, name: String, choice: String) = mapOf("questionId" to question(d, name), "kind" to "CHOICE", "choiceId" to choice)
	private fun any(d: JsonNode, name: String) = mapOf("questionId" to question(d, name), "kind" to "ANY")
	private fun range(d: JsonNode, name: String, min: Int, max: Int) = mapOf("questionId" to question(d, name), "kind" to "NUMBER_RANGE", "min" to min, "max" to max)
	private fun preview(token: String, url: String, answers: List<Map<String, Any>>, extra: Map<String, Any> = emptyMap(), revision: Long = 1) = data(postAs(token, "$url/preview", mapOf("expectedLockVersion" to revision, "answers" to answers) + extra))

	@Test
	@DisplayName("빈 시나리오를 만들 때 샘플 없이 초안과 감사 주체를 반환한다")
	fun createsEmptyDraftAndRetainsSlotsWhenDisabled() {
		val token = seed()
		val s = create(token)
		val url = versionUrl(s)
		val empty = data(getAs(token, url))
		assertThat(empty.at("/definition/questions").size()).isZero()
		assertThat(empty.at("/definition/startQuestionId").isNull).isTrue()
		assertThat(s.at("/scenario/createPrincipal/type").asText()).isEqualTo("ADMIN")
		assertThat(s.at("/scenario/createAt").isTextual).isTrue()
		assertThat(publish(token, url, 0)).hasStatus(HttpStatus.BAD_REQUEST)
		save(token, url, empty["definition"])
		val updated = data(putAs(token, scenarioUrl(s), mapOf("expectedLockVersion" to 0, "title" to "중지", "enabled" to false)))
		assertThat(updated["lockVersion"].asLong()).isEqualTo(1)
		assertThat(postAs(token, base, mapOf("slot" to 1, "title" to "중복"))).hasStatus(HttpStatus.CONFLICT)
		create(token, 2)
		create(token, 3)
		assertThat(data(getAs(token, base)).size()).isEqualTo(3)
		assertThat(postAs(token, base, mapOf("slot" to 4, "title" to "초과"))).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(putAs(token, scenarioUrl(s), mapOf("expectedLockVersion" to 0, "title" to "오래됨", "enabled" to true))).hasStatus(HttpStatus.CONFLICT)
		assertThat(getAs(token, scenarioUrl(s).replace("$group", "${group + 1}"))).hasStatus(HttpStatus.NOT_FOUND)
	}

	@Test
	@DisplayName("같은 속성을 다시 쓰고 스킵할 때 질문별 조건을 보존하고 교집합으로 검색한다")
	fun branchesSkipAndRepeatedPriceConditionsIntersect() {
		val token = seed()
		val s = create(token)
		val url = versionUrl(s)
		val d = save(token, url, definition())
		assertThat(d.at("/definition/questions").count { it["attributeId"]?.asLong() == price }).isEqualTo(2)
		val answers = listOf(choice(d, "q1", "tried"), range(d, "q2c", 9000, 25000), choice(d, "q3", "SINGLE_MALT"), any(d, "q4a"), range(d, "q5", 15000, 30000), any(d, "q6"), any(d, "q7"))
		val result = preview(token, url, answers)
		assertThat(result["completed"].asBoolean()).isTrue()
		assertThat(result.at("/candidates/items").map { it["alcoholId"].asLong() }).containsExactly(810002L)
		assertThat(result["contributions"].filter { it["attributeId"]?.asLong() == price }.map { it["questionId"].asLong() }).contains(question(d, "q2c"), question(d, "q5"))
		val skipped = preview(token, url, listOf(choice(d, "q1", "tried"), range(d, "q2c", 10000, 30000), choice(d, "q3", "BOURBON"), any(d, "q4b"), any(d, "q6"), any(d, "q7")))
		assertThat(skipped["path"].map { it.asLong() }).doesNotContain(question(d, "q5"), question(d, "q4a"), question(d, "q4c"))
		assertThat(skipped.at("/candidates/items").map { it["alcoholId"].asLong() }).containsExactly(810003L)
		assertThat(skipped["contributions"].map { it["questionId"].asLong() }).contains(question(d, "q2c"))
		val expert = preview(token, url, listOf(choice(d, "q1", "expert")))
		assertThat(expert["nextQuestionId"].asLong()).isEqualTo(question(d, "q3"))
		val contradictory = preview(token, url, listOf(choice(d, "q1", "tried"), range(d, "q2c", 10000, 10000), choice(d, "q3", "SINGLE_MALT"), any(d, "q4a"), range(d, "q5", 20000, 30000)))
		assertThat(contradictory.at("/candidates/totalCount").asLong()).isZero()
		data(publish(token, url))
		assertThat(preview(token, url, answers, revision = 2)["path"]).isEqualTo(result["path"])
	}

	@Test
	@DisplayName("앞 답변을 변경하거나 과거 카드를 볼 때 이후 조건을 제거하고 직전 조건만 집계한다")
	fun replacesAnswersAndCountsPastCardsUsingOnlyTheirPrefix() {
		val token = seed()
		val s = create(token)
		val url = versionUrl(s)
		val d = save(token, url, definition())
		val answers = listOf(choice(d, "q1", "tried"), range(d, "q2c", 10000, 30000), choice(d, "q3", "SINGLE_MALT"), any(d, "q4a"), range(d, "q5", 20000, 20000), any(d, "q6"), any(d, "q7"))
		val past = preview(token, url, answers, mapOf("viewQuestionId" to question(d, "q2c")))
		assertThat(past.at("/candidates/totalCount").asLong()).isEqualTo(3)
		assertThat(past["contributions"].map { it["questionId"].asLong() }).containsExactly(question(d, "q1"))
		val changed = preview(token, url, listOf(choice(d, "q1", "expert")) + answers.drop(1), mapOf("changedQuestionId" to question(d, "q1")))
		assertThat(changed["answers"].size()).isEqualTo(1)
		assertThat(changed["discardedQuestionIds"].size()).isEqualTo(6)
		assertThat(changed["nextQuestionId"].asLong()).isEqualTo(question(d, "q3"))
		assertThat(postAs(token, "$url/preview", mapOf("expectedLockVersion" to 1, "answers" to listOf(choice(d, "q1", "expert")) + answers.drop(1)))).hasStatus(HttpStatus.BAD_REQUEST)
	}

	@Test
	@DisplayName("게시본을 복제할 때 새 ID와 참조를 발급하고 이전 게시본과 슬롯을 보존한다")
	fun publishedVersionsAreImmutableAndClonedWithFreshReferences() {
		val token = seed()
		val s = create(token)
		val url = versionUrl(s)
		val d = save(token, url, oneQuestion())
		val published = data(publish(token, url))
		assertThat(published.at("/scenario/activeVersionId").asLong()).isEqualTo(d.at("/version/id").asLong())
		assertThat(putAs(token, url, mapOf("expectedLockVersion" to 2, "definition" to d["definition"]))).hasStatus(HttpStatus.CONFLICT)
		assertThat(deleteAs(token, "${scenarioUrl(s)}?expectedLockVersion=1")).hasStatus(HttpStatus.CONFLICT)
		assertThat(deleteAs(token, "$url?expectedScenarioLockVersion=1&expectedLockVersion=2")).hasStatus(HttpStatus.CONFLICT)
		val clone = data(postAs(token, "${scenarioUrl(s)}/versions", mapOf("expectedScenarioLockVersion" to 1, "sourceVersionId" to d.at("/version/id").asLong())))
		assertThat(clone.at("/definition/startQuestionId").asLong()).isNotEqualTo(d.at("/definition/startQuestionId").asLong())
		assertThat(clone.at("/definition/startQuestionId").asLong()).isEqualTo(clone.at("/definition/questions/0/id").asLong())
		val newUrl = "${scenarioUrl(s)}/versions/${clone.at("/version/id").asLong()}"
		val newer = save(token, newUrl, clone["definition"])
		data(publish(token, newUrl, newer.at("/version/lockVersion").asLong(), 2))
		val old = data(getAs(token, url))
		assertThat(old["definition"]).isEqualTo(d["definition"])
		assertThat(old.at("/version/status").asText()).isEqualTo("PUBLISHED")
		assertThat(putAs(token, scenarioUrl(s), mapOf("expectedLockVersion" to 3, "title" to "중지", "enabled" to false))).hasStatusOk()
	}

	@Test
	@DisplayName("초안 질문이나 버전을 삭제할 때 역참조를 정리하고 부모와 다른 버전을 유지한다")
	fun deletesDraftChildrenAndClearsStartPointers() {
		val token = seed()
		val s = create(token)
		val url = versionUrl(s)
		val d = save(token, url, definition())
		val empty = mapper.valueToTree<ObjectNode>(AlcoholScenarioTestFactory.oneQuestion()).apply {
			putNull("startQuestionId")
			putArray("stages")
			putArray("questions")
			putArray("transitions")
		}
		val cleared = save(token, url, empty, 1)
		assertThat(cleared.at("/version/lockVersion").asLong()).isEqualTo(2)
		assertThat(scalar("SELECT COUNT(*) FROM alcohol_attribute_scenario_questions WHERE version_id=?", d.at("/version/id").asLong())).isZero()
		val second = data(postAs(token, "${scenarioUrl(s)}/versions", mapOf("expectedScenarioLockVersion" to 0)))
		val secondUrl = "${scenarioUrl(s)}/versions/${second.at("/version/id").asLong()}"
		assertThat(deleteAs(token, "$secondUrl?expectedScenarioLockVersion=1&expectedLockVersion=0")).hasStatusOk()
		assertThat(getAs(token, url)).hasStatusOk()
		assertThat(deleteAs(token, "${scenarioUrl(s)}?expectedLockVersion=2")).hasStatusOk()
		assertThat(scalar("SELECT COUNT(*) FROM alcohol_attribute_scenario_versions WHERE scenario_id=?", s.at("/scenario/id").asLong())).isZero()
		assertThat(scalar("SELECT COUNT(*) FROM alcohol_attribute_groups WHERE id=?", group)).isEqualTo(1)
		create(token)
	}

	@Test
	@DisplayName("게시한 속성을 삭제·이동·변경하려 할 때 거절하고 초안만 참조하면 포인터와 revision을 정리한다")
	fun protectsPublishedExternalAttributesAndRepairsDraftCascadePointers() {
		val token = seed()
		val s = create(token)
		val url = versionUrl(s)
		val d = save(token, url, definition())
		val attrUrl = "/v1/alcohols/attributes/$experience"
		assertThat(deleteAs(token, attrUrl)).hasStatusOk()
		val revised = data(getAs(token, url))
		assertThat(revised.at("/definition/startQuestionId").isNull).isTrue()
		assertThat(revised.at("/version/lockVersion").asLong()).isEqualTo(2)
		assertThat(putAs(token, url, mapOf("expectedLockVersion" to 1, "definition" to d["definition"]))).hasStatus(HttpStatus.BAD_REQUEST)
		val categoryOnly = save(token, url, oneQuestion(), 2)
		data(publish(token, url, categoryOnly.at("/version/lockVersion").asLong()))
		assertThat(deleteAs(token, "/v1/alcohols/attribute-groups/$group")).hasStatus(HttpStatus.CONFLICT)
		val s2 = create(token, 2)
		val url2 = versionUrl(s2)
		val graph = oneQuestion()
		val q = graph.at("/questions/0") as ObjectNode
		q.put("sourceKind", "ATTRIBUTE").put("attributeId", price).put("inputKind", "NUMBER_RANGE")
		q.set<JsonNode>("inputConfig", mapper.valueToTree(AlcoholScenarioTestFactory.range(0, 100000, 1000)))
		save(token, url2, graph)
		data(publish(token, url2))
		assertThat(deleteAs(token, "/v1/alcohols/attributes/$price")).hasStatus(HttpStatus.CONFLICT)
		val update = mapOf("displayName" to "시나리오 가격", "type" to "PRICE", "datatype" to "INT", "groupId" to group + 1)
		assertThat(putAs(token, "/v1/alcohols/attributes/$price", update)).hasStatus(HttpStatus.CONFLICT)
		assertThat(putAs(token, "/v1/alcohols/attributes/$price", update - "groupId" + ("datatype" to "DOUBLE"))).hasStatus(HttpStatus.CONFLICT)
		assertThat(scalar("SELECT group_id FROM alcohol_attributes WHERE id=?", price)).isEqualTo(group)
	}

	@Test
	@DisplayName("초안에서 순서를 맞바꿀 때 기존 ID를 유지하고 오래된 저장과 게시를 거절한다")
	fun reordersWithoutChangingIdsAndRejectsStaleGraphOrPublish() {
		val token = seed()
		val s = create(token)
		val url = versionUrl(s)
		val d = save(token, url, definition())
		val graph = d["definition"].deepCopy<ObjectNode>()
		(graph.at("/questions/1") as ObjectNode).put("branchOrder", 2)
		(graph.at("/questions/2") as ObjectNode).put("branchOrder", 1)
		val saved = save(token, url, graph, 1)
		assertThat(saved.at("/definition/questions").map { it["id"].asLong() }).containsExactlyInAnyOrderElementsOf(d.at("/definition/questions").map { it["id"].asLong() })
		assertThat(putAs(token, url, mapOf("expectedLockVersion" to 1, "definition" to d["definition"]))).hasStatus(HttpStatus.CONFLICT)
		assertThat(publish(token, url, 1)).hasStatus(HttpStatus.CONFLICT)
		assertThat(publish(token, url, 2, 9)).hasStatus(HttpStatus.CONFLICT)
		assertThat(data(getAs(token, url)).at("/version/lockVersion").asLong()).isEqualTo(2)
		assertThat(data(getAs(token, scenarioUrl(s))).at("/scenario/lockVersion").asLong()).isZero()
	}

	@Test
	@DisplayName("기본정보와 맛 그룹을 재사용할 때 모든 질문의 조건을 AND로 합성한다")
	fun repeatedBaseSourcesUseAndRatherThanOverwriting() {
		val token = seed()
		val s = create(token)
		val url = versionUrl(s)
		val graph = definition()
		val q7 = graph.at("/questions/10") as ObjectNode
		q7.put("sourceKind", "CATEGORY").putNull("attributeId")
		q7.set<JsonNode>("inputConfig", mapper.valueToTree(AlcoholScenarioTestFactory.choices("SINGLE_MALT", "BOURBON")))
		val d = save(token, url, graph)
		val category = preview(token, url, listOf(choice(d, "q1", "expert"), choice(d, "q3", "SINGLE_MALT"), any(d, "q4a"), any(d, "q5"), any(d, "q6"), choice(d, "q7", "BOURBON")))
		assertThat(category.at("/candidates/totalCount").asLong()).isZero()
		q7.put("sourceKind", "TASTING_GROUP")
		q7.set<JsonNode>("inputConfig", mapper.valueToTree(AlcoholScenarioTestFactory.choices("PEAT", "SWEET")))
		val current = d["definition"].deepCopy<ObjectNode>()
		(current["questions"] as com.fasterxml.jackson.databind.node.ArrayNode).set(
			10,
			q7.deepCopy().apply {
				put("id", question(d, "q7"))
				put("stageId", d.at("/definition/questions/10/stageId").asLong())
			}
		)
		val d2 = save(token, url, current, 1)
		val tasting = preview(token, url, listOf(choice(d2, "q1", "expert"), choice(d2, "q3", "SINGLE_MALT"), choice(d2, "q4a", "PEAT"), any(d2, "q5"), any(d2, "q6"), choice(d2, "q7", "SWEET")), revision = 2)
		assertThat(tasting.at("/candidates/items").map { it["alcoholId"].asLong() }).containsExactly(810002L)
		val abv = preview(token, url, listOf(choice(d2, "q1", "expert"), choice(d2, "q3", "BOURBON"), range(d2, "q4b", 45, 60), range(d2, "q6", 0, 45)), revision = 2)
		assertThat(abv.at("/candidates/totalCount").asLong()).isZero()
	}

	@Test
	@DisplayName("범위 조건으로 이동할 때 GTE는 하한을 LTE는 상한을 양끝 포함으로 비교한다")
	fun numericBranchConditionsCompareTheDeclaredRangeEdges() {
		val token = seed()
		val s = create(token)
		val url = versionUrl(s)
		val graph = mapper.readTree(
			"""
      {"definitionSchemaVersion":1,"startQuestionId":-11,
       "stages":[{"id":-1,"position":1},{"id":-2,"position":2}],
       "questions":[
        {"id":-11,"stageId":-1,"branchOrder":1,"title":"q1","sourceKind":"ABV","inputKind":"NUMBER_RANGE","inputConfig":{"allowAny":true,"min":0,"max":100,"step":1}},
        {"id":-21,"stageId":-2,"branchOrder":1,"title":"q2a","sourceKind":"CATEGORY","inputKind":"SINGLE_CHOICE","inputConfig":{"allowAny":true,"choices":[{"id":"malt","label":"몰트","value":"SINGLE_MALT"}]}},
        {"id":-22,"stageId":-2,"branchOrder":2,"title":"q2b","sourceKind":"CATEGORY","inputKind":"SINGLE_CHOICE","inputConfig":{"allowAny":true,"choices":[{"id":"malt","label":"몰트","value":"SINGLE_MALT"}]}}],
       "transitions":[
        {"id":-101,"fromQuestionId":-11,"priority":1,"predicate":{"operator":"GTE","value":50},"targetKind":"QUESTION","targetQuestionId":-21},
        {"id":-102,"fromQuestionId":-11,"priority":2,"predicate":{"operator":"LTE","value":40},"targetKind":"QUESTION","targetQuestionId":-22},
        {"id":-103,"fromQuestionId":-11,"priority":3,"predicate":{"operator":"ALWAYS"},"targetKind":"END"},
        {"id":-104,"fromQuestionId":-21,"priority":1,"predicate":{"operator":"ALWAYS"},"targetKind":"END"},
        {"id":-105,"fromQuestionId":-22,"priority":1,"predicate":{"operator":"ALWAYS"},"targetKind":"END"}]}
    """
		)
		val d = save(token, url, graph)
		assertThat(preview(token, url, listOf(range(d, "q1", 50, 60)))["nextQuestionId"].asLong()).isEqualTo(question(d, "q2a"))
		assertThat(preview(token, url, listOf(range(d, "q1", 20, 40)))["nextQuestionId"].asLong()).isEqualTo(question(d, "q2b"))
		assertThat(preview(token, url, listOf(range(d, "q1", 20, 60)))["completed"].asBoolean()).isTrue()
		assertThat(preview(token, url, listOf(any(d, "q1")))["completed"].asBoolean()).isTrue()
		val bad = listOf(
			mapOf("questionId" to question(d, "q1"), "kind" to "NUMBER_RANGE", "min" to 51.5, "max" to 60),
			mapOf("questionId" to question(d, "q1"), "kind" to "CHOICE", "choiceId" to "50"),
			mapOf("questionId" to question(d, "q1"), "kind" to "NUMBER_RANGE", "min" to 101, "max" to 110)
		)
		for (answer in bad) assertThat(postAs(token, "$url/preview", mapOf("expectedLockVersion" to 1, "answers" to listOf(answer)))).hasStatus(HttpStatus.BAD_REQUEST)
	}

	@Test
	@DisplayName("게시한 선택값 계약을 수정하거나 배치로 삭제할 때 보존하고 문구만 바꾸면 허용한다")
	fun protectsPublishedStringRulesInIndividualAndBulkWrites() {
		val token = seed()
		val s = create(token)
		val url = versionUrl(s)
		save(token, url, definition())
		data(publish(token, url))
		val attr = mapOf("key" to "scenario_experience", "displayName" to "표시 문구 변경", "type" to "KEYWORD", "datatype" to "STRING")
		assertThat(putAs(token, "/v1/alcohols/attributes/$experience", attr)).hasStatusOk()
		assertThat(putAs(token, "/v1/alcohols/attributes/$experience", attr + ("allowedValues" to listOf("newbie", "couple", "tried", "expert", "extra")))).hasStatus(HttpStatus.CONFLICT)
		assertThat(postAs(token, "/v1/alcohols/attributes/batch", mapOf("items" to listOf(attr + ("groupId" to group + 1))))).hasStatus(HttpStatus.CONFLICT)
		assertThat(postAs(token, "/v1/alcohols/attributes/batch/delete", mapOf("ids" to listOf(experience, price)))).hasStatus(HttpStatus.CONFLICT)
		assertThat(scalar("SELECT COUNT(*) FROM alcohol_attribute_scenario_questions WHERE attribute_id=?", experience)).isEqualTo(4)
		assertThat(deleteAs(token, "/v1/tasting/groups/PEAT")).hasStatus(HttpStatus.CONFLICT)
		val draft = data(postAs(token, "${scenarioUrl(s)}/versions", mapOf("expectedScenarioLockVersion" to 1)))
		val newerUrl = "${scenarioUrl(s)}/versions/${draft.at("/version/id").asLong()}"
		save(token, newerUrl, oneQuestion())
		data(publish(token, newerUrl, 1, 2))
		assertThat(putAs(token, scenarioUrl(s), mapOf("expectedLockVersion" to 3, "title" to "중지", "enabled" to false))).hasStatusOk()
		assertThat(deleteAs(token, "/v1/alcohols/attributes/$experience")).hasStatus(HttpStatus.CONFLICT)
	}

	@Test
	@DisplayName("속성 이동과 게시가 경합할 때 게시 정의가 다른 그룹을 참조하지 않게 한다")
	fun concurrentExternalChangeAndPublicationCannotBreakReferences() {
		val token = seed()
		val s = create(token)
		val url = versionUrl(s)
		save(token, url, definition())
		val update = mapOf("displayName" to "시나리오 가격", "type" to "PRICE", "datatype" to "INT", "groupId" to group + 1)
		val statuses = concurrently({ publish(token, url).response.status }, { putAs(token, "/v1/alcohols/attributes/$price", update).response.status })
		assertThat(statuses).satisfiesAnyOf(
			{ assertThat(it).containsExactly(200, 409) },
			{ assertThat(it).containsExactly(409, 200) }
		)
		val v = data(getAs(token, url))
		if (v.at("/version/status").asText() == "PUBLISHED") {
			assertThat(scalar("SELECT group_id FROM alcohol_attributes WHERE id=?", price)).isEqualTo(group)
		}
	}

	@Test
	@DisplayName("게시 선택지의 맛 그룹 태그를 삭제하거나 의미를 바꿀 때 CASCADE 전에 거절한다")
	fun protectsPublishedTastingGroupMembershipFromTagCascade() {
		val token = seed()
		val s = create(token)
		val url = versionUrl(s)
		jdbc.update("INSERT INTO tasting_tags(id,kor_name,eng_name) VALUES(910003,'미연결 핵심 태그','Unmapped Core')")
		jdbc.update("INSERT INTO tasting_tag_group_tags(group_id,tasting_tag_id,role) VALUES(910001,910003,'CORE')")
		save(token, url, definition())
		data(publish(token, url))
		val deleted = deleteAs(token, "/v1/tasting-tags/910003")
		assertThat(deleted).hasStatus(HttpStatus.CONFLICT)
		assertThat(mapper.readTree(deleted.response.contentAsString).at("/errors/0/code").asText()).isEqualTo("SCENARIO_REFERENCE_IN_USE")
		assertThat(deleteAs(token, "/v1/tasting/groups/PEAT/tags/910003")).hasStatus(HttpStatus.CONFLICT)
		assertThat(putAs(token, "/v1/tasting/groups/PEAT/tags/910003", mapOf("role" to "PERIPHERAL"))).hasStatus(HttpStatus.CONFLICT)
		assertThat(scalar("SELECT COUNT(*) FROM tasting_tag_group_tags WHERE group_id=910001 AND tasting_tag_id=910003")).isEqualTo(1)
	}
	private fun concurrently(first: () -> Int, second: () -> Int): List<Int> {
		val executor = Executors.newFixedThreadPool(2)
		val ready = CountDownLatch(2)
		val start = CountDownLatch(1)
		return try {
			val futures = listOf(first, second).map { action ->
				executor.submit(
					Callable {
						ready.countDown()
						check(start.await(10, TimeUnit.SECONDS))
						action()
					}
				)
			}
			check(ready.await(10, TimeUnit.SECONDS))
			start.countDown()
			futures.map { it.get(30, TimeUnit.SECONDS) }
		} finally {
			executor.shutdownNow()
		}
	}

	@Test
	@DisplayName("슬롯을 동시에 생성할 때 하나만 저장하고 나머지는 정상 충돌로 응답한다")
	fun concurrentSlotCreationHasOneWinner() {
		val token = seed()
		val action = { postAs(token, base, mapOf("slot" to 1, "title" to "동시 생성")).response.status }
		assertThat(concurrently(action, action)).containsExactlyInAnyOrder(200, 409)
		assertThat(data(getAs(token, base)).size()).isEqualTo(1)
	}

	@Test
	@DisplayName("동일 revision을 동시에 저장할 때 하나만 반영한다")
	fun concurrentSavesHaveOneWinner() {
		val token = seed()
		val s = create(token)
		val url = versionUrl(s)
		val action = { putAs(token, url, mapOf("expectedLockVersion" to 0, "definition" to oneQuestion())).response.status }
		assertThat(concurrently(action, action)).containsExactlyInAnyOrder(200, 409)
		assertThat(data(getAs(token, url)).at("/version/lockVersion").asLong()).isEqualTo(1)
	}

	@Test
	@DisplayName("저장과 게시 또는 두 게시가 경합할 때 같은 revision은 한 번만 반영한다")
	fun concurrentSaveAndPublishAndTwoPublicationsAreAtomic() {
		val token = seed()
		val s = create(token)
		val url = versionUrl(s)
		val d = save(token, url, oneQuestion())
		val statuses = concurrently({ putAs(token, url, mapOf("expectedLockVersion" to 1, "definition" to d["definition"])).response.status }, { publish(token, url).response.status })
		assertThat(statuses).containsExactlyInAnyOrder(200, 409)
		val after = data(getAs(token, url))
		assertThat(after.at("/version/lockVersion").asLong()).isEqualTo(2)
		val s2 = create(token, 2)
		val url2 = versionUrl(s2)
		save(token, url2, oneQuestion())
		val action = { publish(token, url2).response.status }
		assertThat(concurrently(action, action)).containsExactlyInAnyOrder(200, 409)
	}

	@Test
	@DisplayName("잘못된 참조나 JSON 또는 인증 없는 요청을 보낼 때 원자적으로 거절한다")
	fun rejectsInvalidShapesReferencesAndUnauthenticatedAccess() {
		val token = seed()
		val s = create(token)
		val url = versionUrl(s)
		assertThat(mockMvcTester.get().uri(base).exchange()).hasStatus(HttpStatus.FORBIDDEN)
		assertThat(mockMvcTester.post().uri(base).contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{}").exchange()).hasStatus(HttpStatus.FORBIDDEN)
		val cases = listOf<(ObjectNode) -> Unit>(
			{ (it.at("/transitions/0/predicate") as ObjectNode).put("operator", 0) },
			{ it.put("definitionSchemaVersion", 2) },
			{ it.put("startQuestionId", 999999) },
			{ (it.at("/questions/0") as ObjectNode).put("stageId", 999999) },
			{ (it.at("/questions/0") as ObjectNode).put("id", 999999) },
			{ (it.at("/questions/0") as ObjectNode).put("attributeId", 999999) },
			{ (it.at("/stages/1") as ObjectNode).put("position", 1) },
			{ (it.at("/questions/2") as ObjectNode).put("branchOrder", 1) },
			{ (it.at("/transitions/1") as ObjectNode).put("priority", 1) },
			{ (it.at("/transitions/0") as ObjectNode).put("targetQuestionId", 999999) },
			{ (it.at("/questions/0/inputConfig") as ObjectNode).put("unsupported", true) },
			{ (it.at("/questions/3/inputConfig") as ObjectNode).put("min", "0") },
			{ (it.at("/questions/0/inputConfig") as ObjectNode).put("allowAny", "true") },
			{ (it.at("/transitions/0/predicate") as ObjectNode).put("operator", "CONTAINS") }
		)
		for (mutate in cases) {
			val graph = definition()
			mutate(graph)
			assertThat(putAs(token, url, mapOf("expectedLockVersion" to 0, "definition" to graph))).hasStatus(HttpStatus.BAD_REQUEST)
			assertThat(data(getAs(token, url)).at("/definition/questions").size()).isZero()
			assertThat(data(getAs(token, url)).at("/version/lockVersion").asLong()).isZero()
		}
		val s2 = create(token, 2)
		val d2 = save(token, versionUrl(s2), oneQuestion())
		assertThat(getAs(token, "${scenarioUrl(s)}/versions/${d2.at("/version/id").asLong()}")).hasStatus(HttpStatus.NOT_FOUND)
		assertThat(putAs(token, url, mapOf("expectedLockVersion" to 0, "definition" to d2["definition"]))).hasStatus(HttpStatus.BAD_REQUEST)
	}
}
