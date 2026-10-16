package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.constant.*
import app.bottlenote.alcohols.domain.*
import app.bottlenote.alcohols.dto.request.*
import app.bottlenote.alcohols.dto.response.*
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.alcohols.service.AdminAlcoholAttributeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@Tag("admin_integration")
@DisplayName("[integration] 공통 속성과 매핑 API")
class AdminAlcoholAttributeIntegrationTest : IntegrationTestSupport() {
	@Autowired private lateinit var alcoholTestFactory: AlcoholTestFactory

	@Autowired private lateinit var attributes: AlcoholAttributeRepository

	@Autowired private lateinit var mappings: AlcoholAttributeMappingRepository

	@Autowired private lateinit var service: AdminAlcoholAttributeService
	private lateinit var token: String
	private val base = "/v1/alcohols/attributes"

	@BeforeEach fun setUp() {
		token = getAccessToken(adminUserTestFactory.persistRootAdmin())
	}
	private fun request(key: String, name: String, value: String = "15000") = AdminAlcoholAttributeCreateRequest(key, name, AttributeType.PRICE, AttributeDatatype.INT, AttributeOperator.EQ, value, "원", "등록 기준")
	private fun create(key: String = "price", name: String = "15,000원"): AdminAlcoholAttributeItem {
		val response = postAs(token, base, request(key, name))
		assertThat(response).hasStatusOk()
		return extractData(response, AdminAlcoholAttributeItem::class.java)
	}
	private fun mapRequest(alcoholId: Long, attributeId: Long) = AdminAlcoholAttributeMappingBulkRequest(listOf(AdminAlcoholAttributeMappingBulkRequest.Item(alcoholId, attributeId)))

	@Test
	@DisplayName("기존 매핑 조회는 요청한 술과 속성의 ID 쌍만 반환한다")
	fun existingMappingsOnlyIncludeRequestedIds() {
		val first = alcoholTestFactory.persistAlcohol()
		val other = alcoholTestFactory.persistAlcohol()
		val a = create("first", "첫 속성")
		val b = create("second", "둘째 속성")
		listOf(first.id to a.id(), first.id to b.id(), other.id to a.id()).forEach { (alcoholId, attributeId) ->
			assertThat(postAs(token, "$base/mappings/batch", mapRequest(alcoholId, attributeId))).hasStatusOk()
		}
		assertThat(mappings.findExistingKeys(listOf(first.id), listOf(a.id())))
			.containsExactly(AlcoholAttributeMappingKey(first.id, a.id()))
	}

	@Test
	@DisplayName("벌크 생성할 때 미연결 속성을 표시명으로 검색하고 감사 정보를 반환한다")
	fun createsDictionaryWithAudit() {
		val response = postAs(token, "$base/batch", AdminAlcoholAttributeBulkRequest(listOf(request("eq", "15,000원"), request("gte", "15,000원 이상"))))
		assertThat(response).hasStatusOk()
		val created = extractData(response, AdminAlcoholAttributeBulkResponse::class.java)
		assertThat(created.created()).isEqualTo(2)
		val result = getAs(token, "$base?keyword=이상")
		assertThat(result).hasStatusOk()
		val items = extractData(result, Array<AdminAlcoholAttributeItem>::class.java)
		assertThat(items).hasSize(1)
		assertThat(items[0].displayName()).isEqualTo("15,000원 이상")
		assertThat(items[0].createAt()).isNotNull()
		assertThat(items[0].createPrincipal().type.name).isEqualTo("ADMIN")
		assertThat(items[0].lastModifyPrincipal().id).isNotNull()
	}

	@Test
	@DisplayName("같은 키를 갱신할 때 ID와 생성 감사 정보를 보존하고 수정 작업자를 기록한다")
	fun upsertsWithoutRecreatingMapping() {
		val a = create()
		val alcohol = alcoholTestFactory.persistAlcohol()
		assertThat(postAs(token, "$base/mappings/batch", mapRequest(alcohol.id, a.id()))).hasStatusOk()
		val before = service.findMappings(AdminAlcoholAttributeMappingSearchRequest(alcohol.id, a.id(), 0, 20)).content.single()
		val persistedCreateAt = attributes.findById(a.id()).orElseThrow().createAt
		val otherToken = getAccessToken(adminUserTestFactory.persistRootAdmin())
		val update = postAs(otherToken, "$base/batch", AdminAlcoholAttributeBulkRequest(listOf(request("price", "30,000원", "30000"))))
		assertThat(update).hasStatusOk()
		assertThat(extractData(update, AdminAlcoholAttributeBulkResponse::class.java).ids()).containsExactly(a.id())
		val after = attributes.findById(a.id()).orElseThrow()
		assertThat(after.createAt).isEqualTo(persistedCreateAt)
		assertThat(after.createPrincipal.id).isEqualTo(a.createPrincipal().id)
		assertThat(after.lastModifyPrincipal.id).isNotEqualTo(a.createPrincipal().id)
		val mapping = service.findMappings(AdminAlcoholAttributeMappingSearchRequest(alcohol.id, a.id(), 0, 20)).content.single()
		assertThat(mapping).isEqualTo(before)
	}

	@Test
	@DisplayName("벌크 중 이름이 충돌할 때 앞선 저장도 롤백한다")
	fun rollsBackBatchOnNameConflict() {
		create("original", "Price")
		val response = postAs(token, "$base/batch", AdminAlcoholAttributeBulkRequest(listOf(request("new_key", "새 이름"), request("other", "price"))))
		assertThat(response).hasStatus(HttpStatus.CONFLICT)
		assertThat(attributes.findByAttributeKey("new_key")).isEmpty()
		assertThat(attributes.findByAttributeKey("original").orElseThrow().displayName).isEqualTo("Price")
	}

	@Test
	@DisplayName("요청에 중복 키나 null 항목이 있을 때 400으로 거부한다")
	fun rejectsDuplicateKeysAndNullItems() {
		assertThat(postAs(token, "$base/batch", AdminAlcoholAttributeBulkRequest(listOf(request("same", "하나"), request(" same ", "둘"))))).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(postAs(token, "$base/batch", mapOf("items" to listOf(null)))).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(postAs(token, "$base/mappings/batch", mapOf("items" to listOf(null)))).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(getAs(token, "$base?alcoholIds=abc")).hasStatus(HttpStatus.BAD_REQUEST)
		assertThat(service.findAll(AdminAlcoholAttributeSearchRequest(null, null, null, 0, 20)).totalElements).isZero()
	}

	@Test
	@DisplayName("매핑이 중복될 때 건너뛰고 감사 정보까지 유지한다")
	fun skipsDuplicateMappingWithoutAuditing() {
		val a = create()
		val alcohol = alcoholTestFactory.persistAlcohol()
		val request = mapRequest(alcohol.id, a.id())
		assertThat(postAs(token, "$base/mappings/batch", request)).hasStatusOk()
		val before = service.findMappings(AdminAlcoholAttributeMappingSearchRequest(alcohol.id, a.id(), 0, 20)).content.single()
		val response = postAs(token, "$base/mappings/batch", AdminAlcoholAttributeMappingBulkRequest(request.items() + request.items()))
		assertThat(response).hasStatusOk()
		val result = extractData(response, AdminAlcoholAttributeCreateResponse::class.java)
		assertThat(result.added()).isZero()
		assertThat(result.skipped()).isEqualTo(2)
		val after = service.findMappings(AdminAlcoholAttributeMappingSearchRequest(alcohol.id, a.id(), 0, 20)).content.single()
		assertThat(after).isEqualTo(before)
		assertThat(after.createPrincipal().type.name).isEqualTo("ADMIN")
	}

	@Test
	@DisplayName("같은 연결을 동시에 등록할 때 한 건만 생성하고 나머지는 건너뛴다")
	fun handlesConcurrentMapping() {
		val a = create()
		val alcohol = alcoholTestFactory.persistAlcohol()
		val request = mapRequest(alcohol.id, a.id())
		val start = CountDownLatch(1)
		val futures = (1..2).map {
			CompletableFuture.supplyAsync {
				start.await()
				service.mapBulk(request)
			}
		}
		start.countDown()
		val results = futures.map { it.get(20, TimeUnit.SECONDS) }
		assertThat(results.sumOf { it.added() }).isEqualTo(1)
		assertThat(results.sumOf { it.skipped() }).isEqualTo(1)
		assertThat(mappings.findAllByAlcoholIdIn(listOf(alcohol.id))).hasSize(1)
	}

	@Test
	@DisplayName("벌크 매핑 대상이 없을 때 요청 전체를 거부한다")
	fun rejectsMissingMappingTarget() {
		val a = create()
		val alcohol = alcoholTestFactory.persistAlcohol()
		val request = AdminAlcoholAttributeMappingBulkRequest(listOf(AdminAlcoholAttributeMappingBulkRequest.Item(alcohol.id, a.id()), AdminAlcoholAttributeMappingBulkRequest.Item(alcohol.id, Long.MAX_VALUE)))
		assertThat(postAs(token, "$base/mappings/batch", request)).hasStatus(HttpStatus.NOT_FOUND)
		assertThat(mappings.findAllByAlcoholIdIn(listOf(alcohol.id))).isEmpty()
	}

	@Test
	@DisplayName("연결을 해제할 때 사전은 유지하고 속성을 삭제할 때 관련 연결도 삭제한다")
	fun separatesMappingAndAttributeDeletion() {
		val a = create()
		val alcohol = alcoholTestFactory.persistAlcohol()
		val request = mapRequest(alcohol.id, a.id())
		val added = postAs(token, "$base/mappings/batch", request)
		assertThat(added).hasStatusOk()
		val id = extractData(added, AdminAlcoholAttributeCreateResponse::class.java).createdIds().single()
		assertThat(deleteAs(token, "$base/mappings/$id")).hasStatusOk()
		assertThat(attributes.findById(a.id())).isPresent()
		assertThat(postAs(token, "$base/mappings/batch", request)).hasStatusOk()
		assertThat(deleteAs(token, "$base/${a.id()}")).hasStatusOk()
		assertThat(mappings.findAllByAlcoholIdIn(listOf(alcohol.id))).isEmpty()
	}

	@Test
	@DisplayName("속성 조회와 집계를 요청할 때 매핑을 기준으로 술을 반환한다")
	fun queriesDictionaryMappingsAndStats() {
		val a = create()
		val alcohol = alcoholTestFactory.persistAlcohol()
		assertThat(postAs(token, "$base/mappings/batch", mapRequest(alcohol.id, a.id()))).hasStatusOk()
		val query = getAs(token, "$base/alcohols?key=price")
		assertThat(query).hasStatusOk()
		assertThat(extractData(query, Array<AdminAttributedAlcoholItem>::class.java).single().alcoholId()).isEqualTo(alcohol.id)
		val summary = getAs(token, "$base/summary?alcoholIds=${alcohol.id}")
		assertThat(summary).hasStatusOk()
		assertThat(extractData(summary, Array<AdminAlcoholAttributeSummaryItem>::class.java).single().registeredCount()).isEqualTo(1)
		assertThat(getAs(token, "$base/keys")).hasStatusOk()
		assertThat(getAs(token, "$base/keywords")).hasStatusOk()
		assertThat(getAs(token, "$base/mappings?attributeId=${a.id()}")).hasStatusOk()
		assertThat(getAs(token, "/v1/alcohols/${alcohol.id}")).hasStatusOk()
	}

	@Test
	@DisplayName("인증 없이 요청할 때 접근을 거부한다")
	fun requiresAuthentication() {
		assertThat(mockMvcTester.get().uri("$base/enums")).hasStatus(HttpStatus.FORBIDDEN)
	}
}
