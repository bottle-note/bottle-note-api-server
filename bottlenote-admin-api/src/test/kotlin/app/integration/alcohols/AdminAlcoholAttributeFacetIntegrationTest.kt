package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeFacets
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeItem
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

@Tag("admin_integration")
class AdminAlcoholAttributeFacetIntegrationTest : IntegrationTestSupport() {
	@Autowired private lateinit var alcoholFactory: AlcoholTestFactory

	@Test fun facetsCountWholeIntersectionAndKeepZeroCandidatesWithPagination() {
		val token = getAccessToken(adminUserTestFactory.persistRootAdmin())
		val base = "/v1/alcohols/attributes"
		fun attribute(key: String): Long = extractData(postAs(token, base, mapOf("key" to key, "displayName" to key, "type" to "PRICE", "datatype" to "INT")), AdminAlcoholAttributeItem::class.java).id()
		val price = attribute("price")
		val score = attribute("score")
		val first = alcoholFactory.persistAlcohol()
		val second = alcoholFactory.persistAlcohol()
		val third = alcoholFactory.persistAlcohol()
		fun mapping(id: Long, attributeId: Long, value: String) {
			assertThat(postAs(token, "$base/mappings/batch", mapOf("items" to listOf(mapOf("alcoholId" to id, "attributeId" to attributeId, "value" to value))))).hasStatusOk()
		}
		mapping(first.id, price, "100")
		mapping(second.id, price, "100")
		mapping(third.id, price, "200")
		mapping(first.id, score, "10")
		mapping(second.id, score, "10")
		mapping(third.id, score, "20")
		val conditions = listOf(mapOf("key" to "price", "operator" to "EQ", "value" to "100"))
		fun facets(extra: Map<String, Any> = emptyMap()): AdminAlcoholAttributeFacets {
			val response = postAs(token, "$base/alcohols/facets", mapOf("conditions" to conditions, "linkedOnly" to true) + extra)
			assertThat(response).hasStatusOk()
			return extractData(response, AdminAlcoholAttributeFacets::class.java)
		}
		val rows = facets().items()
		assertThat(rows.single { it.attribute().id() == score && it.value() == "10" }.alcoholCount()).isEqualTo(2)
		assertThat(rows.single { it.attribute().id() == score && it.value() == "20" }.alcoholCount()).isZero()
		val impossible = facets(mapOf("conditions" to (conditions + mapOf("key" to "score", "operator" to "GT", "value" to "10"))))
		assertThat(impossible.items()).hasSize(4).allMatch { it.alcoholCount() == 0L }
		val union = facets(mapOf("match" to "OR", "conditions" to (conditions + mapOf("key" to "score", "operator" to "EQ", "value" to "20"))))
		assertThat(union.items().single { it.attribute().id() == score && it.value() == "20" }.alcoholCount()).isEqualTo(1)
		val page = facets(mapOf("size" to 1))
		assertThat(page.items()).hasSize(1)
		assertThat(page.items().single().alcoholCount()).isEqualTo(2)
		assertThat(page.hasNext()).isTrue()
		assertThat(facets(mapOf("size" to 1, "page" to 1)).items().single()).isNotEqualTo(page.items().single())
		assertThat(facets(mapOf("groupIds" to listOf(99999999))).items()).isEmpty()
		assertThat(postAs(token, "$base/alcohols/facets", mapOf("conditions" to listOf(mapOf("key" to "price", "operator" to "CONTAINS", "value" to "1"))))).hasStatus(HttpStatus.BAD_REQUEST)
	}
}
