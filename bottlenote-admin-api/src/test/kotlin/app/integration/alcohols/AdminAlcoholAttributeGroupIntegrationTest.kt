package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.domain.AlcoholAttributeRepository
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeGroupItem
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeItem
import app.bottlenote.alcohols.dto.response.AdminAttributedAlcoholItem
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

@Tag("admin_integration")
class AdminAlcoholAttributeGroupIntegrationTest : IntegrationTestSupport() {
	@Autowired private lateinit var alcoholFactory: AlcoholTestFactory

	@Autowired private lateinit var attributes: AlcoholAttributeRepository
	private lateinit var token: String
	private val groups = "/v1/alcohols/attribute-groups"
	private val base = "/v1/alcohols/attributes"

	@BeforeEach fun setUp() {
		token = getAccessToken(adminUserTestFactory.persistRootAdmin())
	}

	private fun group(name: String, order: Int = 0): AdminAlcoholAttributeGroupItem {
		val response = postAs(token, groups, mapOf("name" to name, "displayOrder" to order))
		assertThat(response).hasStatusOk()
		return extractData(response, AdminAlcoholAttributeGroupItem::class.java)
	}
	private fun attributeBody(key: String, extra: Map<String, Any?> = emptyMap()) = mapOf("key" to key, "displayName" to key, "type" to "PRICE", "datatype" to "INT") + extra
	private fun attribute(key: String, groupId: Long? = null): AdminAlcoholAttributeItem {
		val response = postAs(token, base, attributeBody(key, mapOf("groupId" to groupId)))
		assertThat(response).hasStatusOk()
		return extractData(response, AdminAlcoholAttributeItem::class.java)
	}
	private fun map(alcoholId: Long, attributeId: Long, value: String = "100") {
		assertThat(postAs(token, "$base/mappings/batch", mapOf("items" to listOf(mapOf("alcoholId" to alcoholId, "attributeId" to attributeId, "value" to value))))).hasStatusOk()
	}

	@Test fun groupsAreOrderedAndAuditedAndCanBeRenamedOrDeleted() {
		val later = group(" 나중 ", 2)
		val first = group("먼저", 1)
		val tied = group("동순위", 1)
		assertThat(later.name()).isEqualTo("나중")
		assertThat(later.createPrincipal()).isNotNull()
		assertThat(extractData(getAs(token, groups), Array<AdminAlcoholAttributeGroupItem>::class.java).map { it.id() }).containsExactly(first.id(), tied.id(), later.id())
		val renamed = putAs(token, "$groups/${later.id()}", mapOf("name" to "수정", "description" to "설명", "displayOrder" to 0))
		assertThat(renamed).hasStatusOk()
		assertThat(extractData(renamed, AdminAlcoholAttributeGroupItem::class.java).description()).isEqualTo("설명")
		assertThat(deleteAs(token, "$groups/${later.id()}")).hasStatusOk()
		assertThat(deleteAs(token, "$groups/${later.id()}")).hasStatus(HttpStatus.NOT_FOUND)
	}

	@Test fun groupValidationAndDuplicatesReturnDomainErrors() {
		val first = group("운영")
		val second = group("다른 그룹")
		assertThat(postAs(token, groups, mapOf("name" to " 운영 "))).hasStatus(HttpStatus.CONFLICT)
		assertThat(putAs(token, "$groups/${second.id()}", mapOf("name" to first.name()))).hasStatus(HttpStatus.CONFLICT)
		listOf(mapOf("name" to " "), mapOf("name" to "가".repeat(101)), mapOf("name" to "새 그룹", "description" to "가".repeat(501)), mapOf("name" to "새 그룹", "displayOrder" to -1)).forEach {
			assertThat(postAs(token, groups, it)).hasStatus(HttpStatus.BAD_REQUEST)
		}
		assertThat(putAs(token, "$groups/99999999", mapOf("name" to "없음"))).hasStatus(HttpStatus.NOT_FOUND)
	}

	@Test fun groupAssignmentPreservesOmittedAndClearsExplicitNullWithoutLosingMappings() {
		val one = group("그룹1")
		val two = group("그룹2")
		val definition = attribute("price", one.id())
		val alcohol = alcoholFactory.persistAlcohol()
		map(alcohol.id, definition.id())
		assertThat(deleteAs(token, "$groups/${one.id()}")).hasStatus(HttpStatus.CONFLICT)
		assertThat(putAs(token, "$base/${definition.id()}", attributeBody("price"))).hasStatusOk()
		assertThat(attributes.findById(definition.id()).orElseThrow().groupId).isEqualTo(one.id())
		assertThat(postAs(token, base, attributeBody("price"))).hasStatusOk()
		assertThat(attributes.findById(definition.id()).orElseThrow().groupId).isEqualTo(one.id())
		assertThat(putAs(token, "$base/${definition.id()}", attributeBody("price", mapOf("groupId" to two.id())))).hasStatusOk()
		assertThat(deleteAs(token, "$groups/${one.id()}")).hasStatusOk()
		assertThat(putAs(token, "$base/${definition.id()}", attributeBody("price", mapOf("groupId" to null)))).hasStatusOk()
		assertThat<Long?>(attributes.findById(definition.id()).orElseThrow().groupId).isNull()
		assertThat(deleteAs(token, "$groups/${two.id()}")).hasStatusOk()
		val result = getAs(token, "$base/alcohols?key=price&value=100")
		assertThat(result).hasStatusOk()
		val item = extractData(result, Array<AdminAttributedAlcoholItem>::class.java).single()
		assertThat(item.alcoholId()).isEqualTo(alcohol.id)
		assertThat(item.attributes().single().value()).isEqualTo("100")
	}

	@Test fun bulkUpsertPreservesMovesAndClearsGroupsAtomically() {
		val one = group("그룹1")
		val two = group("그룹2")
		val keep = attribute("keep", one.id())
		val move = attribute("move", one.id())
		val clear = attribute("clear", one.id())
		assertThat(postAs(token, "$base/batch", mapOf("items" to listOf(attributeBody("keep"), attributeBody("move", mapOf("groupId" to two.id())), attributeBody("clear", mapOf("groupId" to null)))))).hasStatusOk()
		assertThat(attributes.findById(keep.id()).orElseThrow().groupId).isEqualTo(one.id())
		assertThat(attributes.findById(move.id()).orElseThrow().groupId).isEqualTo(two.id())
		assertThat<Long?>(attributes.findById(clear.id()).orElseThrow().groupId).isNull()
		assertThat(postAs(token, "$base/batch", mapOf("items" to listOf(attributeBody("keep", mapOf("groupId" to two.id())), attributeBody("invalid", mapOf("groupId" to 99999999)))))).hasStatus(HttpStatus.NOT_FOUND)
		assertThat(attributes.findById(keep.id()).orElseThrow().groupId).isEqualTo(one.id())
		assertThat(attributes.findByAttributeKey("invalid")).isEmpty()
	}

	@Test fun invalidAssignmentDoesNotChangeAttribute() {
		val definition = attribute("price")
		assertThat(putAs(token, "$base/${definition.id()}", attributeBody("price", mapOf("groupId" to 99999999)))).hasStatus(HttpStatus.NOT_FOUND)
		listOf(0, -1).forEach {
			assertThat(postAs(token, base, attributeBody("invalid", mapOf("groupId" to it)))).hasStatus(HttpStatus.BAD_REQUEST)
		}
		assertThat<Long?>(attributes.findById(definition.id()).orElseThrow().groupId).isNull()
	}

	@Test fun multipleGroupsUseOrAndPreserveFullWhiskyAttributesAndPagination() {
		val one = group("그룹1")
		val two = group("그룹2")
		val price = attribute("price", one.id())
		val score = attribute("score", two.id())
		val ungrouped = attribute("ungrouped")
		val first = alcoholFactory.persistAlcohol()
		val second = alcoholFactory.persistAlcohol()
		val third = alcoholFactory.persistAlcohol()
		alcoholFactory.persistAlcohol()
		map(first.id, price.id())
		map(first.id, score.id())
		map(first.id, ungrouped.id())
		map(second.id, score.id(), "200")
		map(third.id, ungrouped.id())
		val query = "groupIds=${one.id()},${two.id()},${one.id()}"
		val dictionary = getAs(token, "$base?$query")
		assertThat(dictionary).hasStatusOk()
		assertThat(extractData(dictionary, Array<AdminAlcoholAttributeItem>::class.java).map { it.id() }).containsExactly(price.id(), score.id())
		val legacy = getAs(token, "$base/alcohols?$query")
		assertThat(legacy).hasStatusOk()
		assertThat(extractData(legacy, Array<AdminAttributedAlcoholItem>::class.java).map { it.alcoholId() }).containsExactlyInAnyOrder(first.id, second.id)
		fun search(extra: Map<String, Any> = emptyMap()): Array<AdminAttributedAlcoholItem> {
			val response = postAs(token, "$base/alcohols/search", mapOf("groupIds" to listOf(one.id(), two.id(), one.id())) + extra)
			assertThat(response).hasStatusOk()
			return extractData(response, Array<AdminAttributedAlcoholItem>::class.java)
		}
		val result = search()
		assertThat(result.map { it.alcoholId() }).containsExactlyInAnyOrder(first.id, second.id)
		assertThat(result.single { it.alcoholId() == first.id }.attributes()).hasSize(3)
		assertThat(search(mapOf("conditions" to listOf(mapOf("key" to "score", "operator" to "GTE", "value" to "150")))).map { it.alcoholId() }).containsExactly(second.id)
		assertThat(search(mapOf("size" to 1, "page" to 0))).hasSize(1)
		assertThat(search(mapOf("size" to 1, "page" to 1))).hasSize(1)
		assertThat(search(mapOf("size" to 1, "page" to 2))).isEmpty()
		assertThat(search(mapOf("groupIds" to listOf(99999999)))).isEmpty()
		assertThat(search(mapOf("groupIds" to emptyList<Long>()))).hasSize(4)
		assertThat(getAs(token, "$base/alcohols?$query&key=score&value=200")).hasStatusOk()
	}

	@Test fun groupFiltersIntersectWithTypeAndCompoundConditions() {
		val one = group("가격 그룹")
		val two = group("메모 그룹")
		val price = attribute("price", one.id())
		val noteResponse = postAs(token, base, attributeBody("note", mapOf("groupId" to two.id(), "type" to "NOTE", "datatype" to "STRING")))
		assertThat(noteResponse).hasStatusOk()
		val note = extractData(noteResponse, AdminAlcoholAttributeItem::class.java)
		val both = alcoholFactory.persistAlcohol()
		val onlyPrice = alcoholFactory.persistAlcohol()
		map(both.id, price.id())
		map(both.id, note.id(), "메모")
		map(onlyPrice.id, price.id())
		fun ids(body: Map<String, Any?>): List<Long> {
			val response = postAs(token, "$base/alcohols/search", body)
			assertThat(response).hasStatusOk()
			return extractData(response, Array<AdminAttributedAlcoholItem>::class.java).map { it.alcoholId() }
		}
		assertThat(ids(mapOf("groupIds" to listOf(one.id()), "type" to "NOTE"))).isEmpty()
		assertThat(ids(mapOf("groupIds" to listOf(one.id(), two.id()), "type" to "NOTE"))).containsExactly(both.id)
		assertThat(ids(mapOf("groupIds" to listOf(one.id()), "conditions" to listOf(mapOf("key" to "note", "operator" to "NOT_EXISTS"))))).containsExactly(onlyPrice.id)
		assertThat(ids(mapOf("groupIds" to listOf(two.id()), "match" to "OR", "conditions" to listOf(mapOf("key" to "price", "operator" to "EXISTS"), mapOf("key" to "note", "operator" to "EXISTS"))))).containsExactly(both.id)
		assertThat(ids(mapOf("groupIds" to listOf(one.id()), "keyword" to "일치하지않는이름"))).isEmpty()
		assertThat(ids(mapOf("groupIds" to null))).containsExactlyInAnyOrder(both.id, onlyPrice.id)
		assertThat(ids(mapOf("groupIds" to listOf(two.id(), 99999999L)))).containsExactly(both.id)
		val legacy = getAs(token, "$base/alcohols?groupIds=${one.id()}&type=NOTE")
		assertThat(legacy).hasStatusOk()
		assertThat(extractData(legacy, Array<AdminAttributedAlcoholItem>::class.java)).isEmpty()
		val repeated = getAs(token, "$base?groupIds=${one.id()}&groupIds=${two.id()}&type=NOTE")
		assertThat(repeated).hasStatusOk()
		assertThat(extractData(repeated, Array<AdminAlcoholAttributeItem>::class.java).map { it.id() }).containsExactly(note.id())
	}

	@Test fun invalidGroupFilterArraysAreRejectedInGetAndPost() {
		listOf(listOf(0L), listOf(-1L), listOf<Long?>(null), (1L..101L).toList()).forEach { ids ->
			val response = postAs(token, "$base/alcohols/search", mapOf("groupIds" to ids))
			assertThat(response).hasStatus(HttpStatus.BAD_REQUEST)
			assertThat(mapper.readTree(response.response.contentAsString).at("/errors/0/code").asText()).isEqualTo(if (ids.size > 100) "ALCOHOL_ATTRIBUTE_GROUP_IDS_SIZE_INVALID" else "ALCOHOL_ATTRIBUTE_GROUP_INVALID_VALUE")
		}
		listOf("0", "-1", (1..101).joinToString(",")).forEach { ids ->
			assertThat(getAs(token, "$base?groupIds=$ids")).hasStatus(HttpStatus.BAD_REQUEST)
			assertThat(getAs(token, "$base/alcohols?groupIds=$ids")).hasStatus(HttpStatus.BAD_REQUEST)
		}
	}

	@Test fun groupEndpointsRequireAuthentication() {
		assertThat(mockMvcTester.get().uri(groups).exchange()).hasStatus(HttpStatus.FORBIDDEN)
		assertThat(mockMvcTester.delete().uri("$groups/1").exchange()).hasStatus(HttpStatus.FORBIDDEN)
	}
}
