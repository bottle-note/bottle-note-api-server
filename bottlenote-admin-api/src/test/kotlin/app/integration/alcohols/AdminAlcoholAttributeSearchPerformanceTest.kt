package app.integration.alcohols

import app.IntegrationTestSupport
import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@EnabledIfEnvironmentVariable(named = "ATTRIBUTE_SEARCH_BENCHMARK", matches = "true")
class AdminAlcoholAttributeSearchPerformanceTest : IntegrationTestSupport() {
	@Autowired private lateinit var jdbc: JdbcTemplate

	@Autowired private lateinit var entityManagerFactory: EntityManagerFactory

	@Test
	fun measureCompoundSearch() {
		val token = getAccessToken(adminUserTestFactory.persistRootAdmin())
		jdbc.batchUpdate(
			"INSERT INTO alcohols(id,kor_name,eng_name,type,kor_category,eng_category,category_group) VALUES(?,?,?,'WHISKY','위스키','WHISKY','SINGLE_MALT')",
			(1..4000).map { arrayOf<Any>(500000L + it, "측정 위스키 $it", "Benchmark $it") }
		)
		jdbc.batchUpdate(
			"INSERT INTO alcohol_attributes(id,attribute_key,display_name,attribute_type,datatype) VALUES(?,?,?,'PRICE','INT')",
			(1..20).map { arrayOf<Any>(500000L + it, "bench_$it", "측정 속성 $it") }
		)
		(1..20).forEach { attribute ->
			jdbc.batchUpdate(
				"INSERT INTO alcohol_attribute_mappings(alcohol_id,attribute_id,attribute_value) VALUES(?,?,?)",
				(1..4000).map { arrayOf<Any>(500000L + it, 500000L + attribute, (it % 100).toString()) }
			)
		}
		// 대량 삽입 직후의 부정확한 통계가 측정 결과를 바꾸지 않도록 갱신한다.
		jdbc.execute("ANALYZE TABLE alcohols, alcohol_attributes, alcohol_attribute_mappings")
		val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
		statistics.isStatisticsEnabled = true
		try {
			fun measure(label: String, body: Map<String, Any>, total: Int, expectedSql: Long) {
				fun invoke(): Double {
					val start = System.nanoTime()
					val response = postAs(token, "/v1/alcohols/attributes/alcohols/search", body)
					val elapsed = (System.nanoTime() - start) / 1_000_000.0
					assertThat(response).hasStatusOk()
					val json = mapper.readTree(response.response.contentAsString)
					assertThat(json["data"].size()).isEqualTo(20)
					assertThat(json.at("/meta/totalElements").asInt()).isEqualTo(total)
					return elapsed
				}
				repeat(10) { invoke() }
				statistics.clear()
				val times = (1..30).map { invoke() }.sorted()
				val sqlCount = statistics.prepareStatementCount
				assertThat(sqlCount).isEqualTo(expectedSql * 30)
				println("ATTRIBUTE_BENCHMARK label=${System.getenv("ATTRIBUTE_SEARCH_BENCHMARK_LABEL")} $label rows=4000 mappings=80000 samples=30 medianMs=${times[15]} p95Ms=${times[28]} sqlPerRequest=${sqlCount / 30.0}")
			}
			listOf(1, 5, 20).forEach { count ->
				val body = mapOf("conditions" to (1..count).map { mapOf("key" to "bench_$it", "operator" to "GTE", "value" to "50") })
				measure("conditions=$count", body, 2000, 7)
			}
			jdbc.batchUpdate("INSERT INTO alcohol_attribute_groups(id,name) VALUES(?,?)", (1..20).map { arrayOf<Any>(500000L + it, "측정 그룹 $it") })
			jdbc.update("UPDATE alcohol_attributes SET group_id = id WHERE id BETWEEN 500001 AND 500020")
			jdbc.execute("ANALYZE TABLE alcohol_attributes, alcohol_attribute_groups, alcohol_attribute_mappings")
			listOf(1, 5, 20).forEach { count ->
				measure("groups=$count", mapOf("groupIds" to (1..count).map { 500000L + it }), 4000, 6)
			}
			measure("conditions=20 groups=20", mapOf("groupIds" to (1..20).map { 500000L + it }, "conditions" to (1..20).map { mapOf("key" to "bench_$it", "operator" to "GTE", "value" to "50") }), 2000, 7)
		} finally {
			statistics.isStatisticsEnabled = false
		}
	}
}
