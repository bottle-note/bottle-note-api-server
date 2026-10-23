package app.integration.alcohols

import app.IntegrationTestSupport
import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate

@DisplayName("속성 그룹 후보를 조회할 때 필터·청크·집계를 함께 검증한다")
@Tag("admin_integration")
class AdminAlcoholCandidateIntegrationTest : IntegrationTestSupport() {
	@Autowired private lateinit var jdbc: JdbcTemplate

	@Autowired private lateinit var emf: EntityManagerFactory
	private val group = 700001L
	private val experience = 700001L
	private val price = 700002L
	private val endpoint = "/v1/attribute/groups/$group/candidates"

	private fun seed(count: Int = 3): String {
		val token = getAccessToken(adminUserTestFactory.persistRootAdmin())
		jdbc.update("INSERT INTO alcohol_attribute_groups(id,name) VALUES(?,?)", group, "추천 시나리오")
		jdbc.update("INSERT INTO alcohol_attribute_groups(id,name) VALUES(?,?)", group + 1, "다른 그룹")
		jdbc.update("INSERT INTO alcohol_attributes(id,group_id,attribute_key,display_name,attribute_type,datatype,allow_multiple_values) VALUES(?,?,'candidate_experience','경험','KEYWORD','STRING',true)", experience, group)
		jdbc.update("INSERT INTO alcohol_attributes(id,group_id,attribute_key,display_name,attribute_type,datatype,allow_multiple_values) VALUES(?,?,'candidate_price','가격','PRICE','INT',true)", price, group)
		jdbc.batchUpdate(
			"INSERT INTO alcohols(id,kor_name,eng_name,type,kor_category,eng_category,category_group,abv,age,cask,volume) VALUES(?,?,?,'WHISKY','싱글몰트','Single Malt','SINGLE_MALT',?,'12','BOURBON','700')",
			(1..count).map { arrayOf<Any>(800000L + it, "후보 $it", "Candidate $it", if (it == 3) "unknown" else "40%") }
		)
		jdbc.batchUpdate(
			"INSERT INTO alcohol_attribute_mappings(alcohol_id,attribute_id,attribute_value) VALUES(?,?,?)",
			(1..count).flatMap { listOf(arrayOf<Any>(800000L + it, experience, "경험 있음"), arrayOf<Any>(800000L + it, price, (it * 10000).toString())) }
		)
		jdbc.update("INSERT INTO alcohol_attribute_mappings(alcohol_id,attribute_id,attribute_value) VALUES(800001,?,'처음')", experience)
		return token
	}
	private fun query(token: String, body: Map<String, Any?> = emptyMap()): JsonNode {
		val result = postAs(token, endpoint, body)
		assertThat(result).hasStatusOk()
		return mapper.readTree(result.response.contentAsString)
	}
	private fun condition(id: Long, operator: String, value: String? = null) = mapOf("attributeId" to id, "operator" to operator, "value" to value)

	@Test fun offsetPagesDoNotRepeatAndFacetsCountAllCandidates() {
		val token = seed()
		val first = query(token, mapOf("size" to 1, "nextQuestion" to mapOf("source" to "ATTRIBUTE", "attributeId" to experience)))
		assertThat(first.at("/data/totalCount").asLong()).isEqualTo(3)
		assertThat(first.at("/data/items").size()).isEqualTo(1)
		assertThat(first.at("/data/facets/options").first { it["value"].asText() == "경험 있음" }["count"].asLong()).isEqualTo(3)
		assertThat(first.at("/data/items/0/attributes").first { it["attributeId"].asLong() == experience }["values"].size()).isEqualTo(2)
		assertThat(first.at("/meta/page").asInt()).isZero()
		assertThat(first.at("/meta/totalElements").asLong()).isEqualTo(3)
		assertThat(first.at("/meta/totalPages").asInt()).isEqualTo(3)
		assertThat(first.at("/meta/hasNext").asBoolean()).isTrue()
		val next = query(token, mapOf("size" to 1, "page" to 1))
		assertThat(next.at("/data/items").map { it["alcoholId"].asLong() }).containsExactly(800002L)
		val last = query(token, mapOf("size" to 1, "page" to 2))
		assertThat(last.at("/data/items").map { it["alcoholId"].asLong() }).containsExactly(800003L)
		assertThat(last.at("/meta/hasNext").asBoolean()).isFalse()
		assertThat(next.at("/data/facets").isNull).isTrue()
		assertThat(next.at("/meta/pagination").isMissingNode).isTrue()
		val empty = query(token, mapOf("page" to 10))
		assertThat(empty.at("/data/items").size()).isZero()
		assertThat(empty.at("/data/totalCount").asLong()).isEqualTo(3)
		assertThat(postAs(token, endpoint, mapOf("page" to -1))).hasStatus(HttpStatus.BAD_REQUEST)
	}

	@Test fun returnsDefinitionsOnceAndKeepsOnlyValuesOnEachCandidate() {
		val result = query(seed())
		val definitions = result.at("/data/attributeDefinitions")
		assertThat(definitions.map { it["attributeId"].asLong() }).containsExactly(experience, price)
		assertThat(definitions.first()["displayName"].asText()).isEqualTo("경험")
		result.at("/data/items").forEach { item ->
			item["attributes"].forEach { attribute ->
				assertThat(attribute.fieldNames().asSequence().toList()).containsExactlyInAnyOrder("attributeId", "values")
			}
		}
	}

	@Test fun matchingOptionsAreNotHiddenByHundredUnmatchedValues() {
		val token = seed(105)
		jdbc.update("UPDATE alcohol_attribute_mappings SET attribute_value=CONCAT('option_', alcohol_id) WHERE attribute_id=? AND attribute_value='경험 있음'", experience)
		val result = query(token, mapOf("include" to listOf(condition(price, "EQ", "1050000")), "nextQuestion" to mapOf("source" to "ATTRIBUTE", "attributeId" to experience)))
		val options = result.at("/data/facets/options")
		assertThat(options.size()).isEqualTo(100)
		assertThat(options.first()["value"].asText()).isEqualTo("option_800105")
		assertThat(options.first()["count"].asLong()).isEqualTo(1)
		assertThat(result.at("/data/facets/hasMoreOptions").asBoolean()).isTrue()
	}

	@Test fun excludesWholeWhiskyEvenWithAnotherValueAndKeepsZeroOptions() {
		val token = seed()
		val result = query(token, mapOf("include" to listOf(condition(experience, "EQ", "경험 있음")), "exclude" to listOf(condition(experience, "EQ", "처음")), "nextQuestion" to mapOf("source" to "ATTRIBUTE", "attributeId" to experience)))
		assertThat(result.at("/data/totalCount").asLong()).isEqualTo(2)
		assertThat(result.at("/data/items").map { it["alcoholId"].asLong() }).containsExactly(800002L, 800003L)
		assertThat(result.at("/data/facets/options").first { it["value"].asText() == "처음" }["count"].asLong()).isZero()
		assertThat(query(token, mapOf("include" to listOf(condition(experience, "NE", "처음")))).at("/data/totalCount").asLong()).isEqualTo(2)
	}

	@Test fun baseFieldsAndCoreTagsAreFilteredWithoutExposingOtherGroupsOrIcons() {
		val token = seed()
		jdbc.update("INSERT INTO tasting_tags(id,kor_name,eng_name,icon) VALUES(900001,'바닐라','Vanilla','private base64'),(900002,'꿀','Honey',null)")
		jdbc.update("INSERT INTO tasting_tag_groups(id,code,name,display_order) VALUES(900001,'VANILLA','바닐라',0)")
		jdbc.update("INSERT INTO tasting_tag_group_tags(group_id,tasting_tag_id,role) VALUES(900001,900001,'CORE'),(900001,900002,'PERIPHERAL')")
		jdbc.update("INSERT INTO alcohols_tasting_tags(alcohol_id,tasting_tag_id) VALUES(800001,900001),(800001,900002),(800002,900002)")
		jdbc.update("INSERT INTO alcohol_attributes(id,group_id,attribute_key,display_name,attribute_type,datatype) VALUES(700003,700002,'private_note','관리 메모','NOTE','STRING')")
		jdbc.update("INSERT INTO alcohol_attribute_mappings(alcohol_id,attribute_id,attribute_value) VALUES(800001,700003,'숨김')")
		val result = query(token, mapOf("categoryGroups" to listOf("SINGLE_MALT"), "abvFrom" to 40, "abvTo" to 40, "tastingGroupCodes" to listOf("VANILLA")))
		assertThat(result.at("/data/totalCount").asLong()).isEqualTo(1)
		assertThat(result.at("/data/items/0/abv").asDouble()).isEqualTo(40.0)
		assertThat(result.at("/data/items/0/age").asText()).isEqualTo("12")
		assertThat(result.at("/data/items/0/tastingTags").size()).isEqualTo(2)
		assertThat(result.toString()).doesNotContain("private base64", "private_note", "createPrincipal")
		val facets = query(token, mapOf("nextQuestion" to mapOf("source" to "TASTING_GROUP")))
		assertThat(facets.at("/data/facets/options/0/count").asLong()).isEqualTo(1)
		assertThat(facets.at("/data/facets/missingCount").asLong()).isEqualTo(2)
	}

	@Test fun validatesInputAndExcludesDeletedOrUnlinkedAlcohols() {
		val token = seed()
		jdbc.update("UPDATE alcohols SET deleted_at=NOW() WHERE id=800003")
		assertThat(query(token).at("/data/totalCount").asLong()).isEqualTo(2)
		assertThat(postAs(token, endpoint, mapOf("size" to 101))).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(postAs(token, endpoint, mapOf("abvFrom" to 50, "abvTo" to 40))).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(postAs(token, endpoint, mapOf("include" to listOf(condition(experience, "GTE", "10"))))).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(postAs(token, endpoint, mapOf("include" to listOf(condition(999999, "EQ", "a"))))).hasStatus(HttpStatus.NOT_FOUND)
		assertThat(postAs(token, endpoint, mapOf("nextQuestion" to mapOf("source" to "ATTRIBUTE")))).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(postAs(token, "/v1/attribute/groups/999999/candidates", emptyMap<String, Any>())).hasStatus(HttpStatus.NOT_FOUND)
		assertThat(postAs(token, "/v1/attribute/groups/999999/candidates", mapOf("abvFrom" to 50, "abvTo" to 40))).hasStatus(HttpStatus.NOT_FOUND)
		assertThat(postAs(token, endpoint, mapOf("nextQuestion" to mapOf("source" to 0)))).hasStatus(HttpStatus.BAD_REQUEST)
		val empty = postAs(token, "/v1/attribute/groups/700002/candidates", emptyMap<String, Any>())
		assertThat(empty).hasStatusOk()
		assertThat(mapper.readTree(empty.response.contentAsString).at("/data/totalCount").asLong()).isZero()
	}

	@Test fun rangeFacetsCountWholeIntersectionAndTreatUnknownAbvAsMissing() {
		val token = seed()
		val facets = query(token, mapOf("size" to 1, "nextQuestion" to mapOf("source" to "ABV", "ranges" to listOf(mapOf("key" to "light", "from" to 0, "to" to 43), mapOf("key" to "strong", "from" to 50)))))
		assertThat(facets.at("/data/facets/options/0/count").asLong()).isEqualTo(2)
		assertThat(facets.at("/data/facets/options/1/count").asLong()).isZero()
		assertThat(facets.at("/data/facets/missingCount").asLong()).isEqualTo(1)
		assertThat(query(token, mapOf("abvFrom" to 0)).at("/data/totalCount").asLong()).isEqualTo(2)
	}

	@Test fun queryCountDoesNotGrowWithPageSize() {
		val token = seed(101)
		val stats = emf.unwrap(SessionFactory::class.java).statistics
		val wasEnabled = stats.isStatisticsEnabled
		stats.isStatisticsEnabled = true
		try {
			val body = mapOf("include" to listOf(condition(price, "GTE", "10000")), "nextQuestion" to mapOf("source" to "ATTRIBUTE", "attributeId" to experience), "size" to 25)
			stats.clear()
			val smallResult = query(token, body)
			val small = stats.prepareStatementCount
			stats.clear()
			val largeResult = query(token, body + ("size" to 100))
			assertThat(stats.prepareStatementCount).isEqualTo(small)
			assertThat(smallResult.at("/data/items").size()).isEqualTo(25)
			assertThat(largeResult.at("/data/items").size()).isEqualTo(100)
			assertThat(largeResult.at("/data/totalCount").asLong()).isEqualTo(101)
		} finally {
			stats.isStatisticsEnabled = wasEnabled
		}
	}

	@Test fun combinesExclusionCategoryAndAbvWithWholeResultFacets() {
		val token = seed(4)
		val result = query(token, mapOf("include" to listOf(condition(price, "GTE", "10000")), "exclude" to listOf(condition(experience, "EQ", "처음")), "categoryGroups" to listOf("SINGLE_MALT"), "abvFrom" to 40, "abvTo" to 50, "nextQuestion" to mapOf("source" to "ABV", "ranges" to listOf(mapOf("key" to "light", "from" to 40, "to" to 43)))))
		assertThat(result.at("/data/items").map { it["alcoholId"].asLong() }).containsExactly(800002L, 800004L)
		assertThat(result.at("/data/facets/options/0/count").asLong()).isEqualTo(2)
	}

	@Test fun numericBoundsMustMatchTheSameMappedValue() {
		val token = seed()
		jdbc.update("INSERT INTO alcohol_attribute_mappings(alcohol_id,attribute_id,attribute_value) VALUES(800001,?,'50000')", price)
		val result = query(token, mapOf("include" to listOf(condition(price, "GTE", "20000"), condition(price, "LTE", "40000")), "nextQuestion" to mapOf("source" to "ATTRIBUTE", "attributeId" to price, "ranges" to listOf(mapOf("key" to "budget", "from" to 20000, "to" to 25000)))))
		assertThat(result.at("/data/items").map { it["alcoholId"].asLong() }).containsExactly(800002L, 800003L)
		assertThat(result.at("/data/facets/options/0/count").asLong()).isEqualTo(1)
	}

	@Test fun handlesMissingAttributesAndRejectsCrossGroupConditionsAndInvalidShapes() {
		val token = seed()
		jdbc.update("DELETE FROM alcohol_attribute_mappings WHERE alcohol_id=800002 AND attribute_id=?", price)
		assertThat(query(token, mapOf("include" to listOf(condition(price, "NOT_EXISTS")))).at("/data/totalCount").asLong()).isEqualTo(1)
		val next = query(token, mapOf("nextQuestion" to mapOf("source" to "ATTRIBUTE", "attributeId" to price)))
		assertThat(next.at("/data/facets/missingCount").asLong()).isEqualTo(1)
		assertThat(postAs(token, endpoint, mapOf("include" to listOf(condition(price, "EXISTS", "10"))))).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(postAs(token, endpoint, mapOf("include" to listOf(null)))).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(postAs(token, endpoint, mapOf("include" to (1..21).map { condition(price, "EXISTS") }))).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(postAs(token, endpoint, mapOf("nextQuestion" to mapOf("source" to "CATEGORY", "attributeId" to experience)))).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(postAs(token, endpoint, mapOf("nextQuestion" to mapOf("source" to "ABV", "ranges" to listOf(mapOf("key" to "bad", "from" to 50, "to" to 30)))))).hasStatus(HttpStatus.BAD_REQUEST)
		jdbc.update("UPDATE alcohol_attributes SET group_id=700002 WHERE id=?", price)
		assertThat(postAs(token, endpoint, mapOf("include" to listOf(condition(price, "GTE", "10"))))).hasStatus(HttpStatus.NOT_FOUND)
	}

	@Test
	@DisplayName("같은 기본 필터의 여러 선택값은 OR로 묶고 서로 다른 필터는 AND로 적용한다")
	fun baseFilterAlternativesRemainOrWithinAnAndIntersection() {
		val token = seed()
		jdbc.update("UPDATE alcohols SET category_group='BOURBON' WHERE id=800002")
		jdbc.update("INSERT INTO tasting_tags(id,kor_name,eng_name) VALUES(900001,'바닐라','Vanilla'),(900002,'꿀','Honey')")
		jdbc.update("INSERT INTO tasting_tag_groups(id,code,name,display_order) VALUES(900001,'VANILLA','바닐라',0),(900002,'HONEY','꿀',1)")
		jdbc.update("INSERT INTO tasting_tag_group_tags(group_id,tasting_tag_id,role) VALUES(900001,900001,'CORE'),(900002,900002,'CORE')")
		jdbc.update("INSERT INTO alcohols_tasting_tags(alcohol_id,tasting_tag_id) VALUES(800001,900001),(800002,900002)")
		val result = query(token, mapOf("categoryGroups" to listOf("SINGLE_MALT", "BOURBON"), "tastingGroupCodes" to listOf("VANILLA", "HONEY"), "abvFrom" to 40, "abvTo" to 40))
		assertThat(result.at("/data/items").map { it["alcoholId"].asLong() }).containsExactly(800001L, 800002L)
		assertThat(result.at("/data/totalCount").asLong()).isEqualTo(2)
		assertThat(query(token, mapOf("tastingGroupCodes" to listOf("HONEY"), "categoryGroups" to listOf("SINGLE_MALT"))).at("/data/totalCount").asLong()).isZero()
	}
}
