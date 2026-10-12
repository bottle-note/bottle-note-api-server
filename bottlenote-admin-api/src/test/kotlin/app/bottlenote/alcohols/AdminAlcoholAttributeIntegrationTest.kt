package app.bottlenote.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.constant.AttributeDatatype
import app.bottlenote.alcohols.constant.AttributeType
import app.bottlenote.alcohols.domain.Alcohol
import app.bottlenote.alcohols.domain.AlcoholAttribute
import app.bottlenote.alcohols.domain.AlcoholAttributeRepository
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeCreateRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeUpdateRequest
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import java.time.LocalDateTime

@Tag("admin_integration")
@DisplayName("[integration] 위스키 속성 어드민 API")
class AdminAlcoholAttributeIntegrationTest : IntegrationTestSupport() {
	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	@Autowired
	private lateinit var alcoholAttributeRepository: AlcoholAttributeRepository

	private lateinit var accessToken: String

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		accessToken = getAccessToken(admin)
	}

	@Test
	@DisplayName("alcoholIds를 쉼표로 넘기면 속성 컨트롤러가 그 병들의 속성을 반환한다")
	fun listsAttributesForCommaSeparatedAlcoholIds() {
		val first = alcoholTestFactory.persistAlcohol()
		val second = alcoholTestFactory.persistAlcohol()
		saveAttribute(first, "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "피트")
		saveAttribute(second, "price_bottle", AttributeType.PRICE, AttributeDatatype.INT, "58000")

		val result =
			mockMvcTester
				.get()
				.uri("/v1/alcohols/attributes")
				.param("alcoholIds", "${first.id},${second.id}")
				.header("Authorization", "Bearer $accessToken")
				.exchange()

		assertThat(result).hasStatusOk()
		assertThat(result).bodyJson().extractingPath("$.data.length()").isEqualTo(2)
		assertThat(result).bodyJson().extractingPath("$.data[0].key").isEqualTo("keyword")
		assertThat(result).bodyJson().extractingPath("$.data[1].value").isEqualTo("58000")
	}

	@Test
	@DisplayName("위스키 상세 경로는 그대로 위스키 조회다")
	fun alcoholDetailRouteStaysOnAlcoholController() {
		val alcohol = alcoholTestFactory.persistAlcohol()

		assertThat(
			mockMvcTester
				.get()
				.uri("/v1/alcohols/${alcohol.id}")
				.header("Authorization", "Bearer $accessToken")
		).hasStatusOk().bodyJson().extractingPath("$.data.korName").isEqualTo(alcohol.korName)
	}

	@Test
	@DisplayName("alcoholIds가 없으면 400이다")
	fun rejectsMissingAlcoholIds() {
		assertThat(
			mockMvcTester
				.get()
				.uri("/v1/alcohols/attributes")
				.header("Authorization", "Bearer $accessToken")
		).hasStatus(HttpStatus.BAD_REQUEST)
	}

	@Test
	@DisplayName("속성을 추가하고 같은 값은 건너뛴다")
	fun createsAndSkipsDuplicate() {
		val alcohol = alcoholTestFactory.persistAlcohol()
		val request =
			AdminAlcoholAttributeCreateRequest(
				listOf(alcohol.id),
				"keyword",
				AttributeType.KEYWORD,
				AttributeDatatype.STRING,
				"피트",
				"추천 필터",
				LocalDateTime.of(2026, 10, 12, 9, 0)
			)

		val created = post("/v1/alcohols/attributes", request)
		assertThat(created).hasStatusOk()
		assertThat(created).bodyJson().extractingPath("$.data.added").isEqualTo(1)
		assertThat(created).bodyJson().extractingPath("$.data.skipped").isEqualTo(0)

		val skipped = post("/v1/alcohols/attributes", request)
		assertThat(skipped).hasStatusOk()
		assertThat(skipped).bodyJson().extractingPath("$.data.added").isEqualTo(0)
		assertThat(skipped).bodyJson().extractingPath("$.data.skipped").isEqualTo(1)
	}

	@Test
	@DisplayName("없는 위스키에 속성을 추가하면 404이다")
	fun missingAlcoholIsNotFound() {
		val request =
			AdminAlcoholAttributeCreateRequest(
				listOf(999999L),
				"keyword",
				AttributeType.KEYWORD,
				AttributeDatatype.STRING,
				"피트",
				null,
				null
			)

		assertThat(post("/v1/alcohols/attributes", request)).hasStatus(HttpStatus.NOT_FOUND)
	}

	@Test
	@DisplayName("속성을 수정하고 한 건과 여러 건을 삭제한다")
	fun updatesAndDeletesAttributes() {
		val alcohol = alcoholTestFactory.persistAlcohol()
		val attribute =
			saveAttribute(alcohol, "price_bottle", AttributeType.PRICE, AttributeDatatype.INT, "10000")

		val updated =
			mockMvcTester
				.put()
				.uri("/v1/alcohols/attributes/${attribute.id}")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					mapper.writeValueAsString(
						AdminAlcoholAttributeUpdateRequest(
							AttributeType.PRICE,
							AttributeDatatype.INT,
							"20000",
							"시세",
							null
						)
					)
				)
				.header("Authorization", "Bearer $accessToken")
				.exchange()
		assertThat(updated).hasStatusOk()
		assertThat(updated).bodyJson().extractingPath("$.data.code").isEqualTo("ALCOHOL_ATTRIBUTE_UPDATED")

		val summary =
			mockMvcTester
				.get()
				.uri("/v1/alcohols/attributes/summary")
				.param("alcoholIds", alcohol.id.toString())
				.header("Authorization", "Bearer $accessToken")
				.exchange()
		assertThat(summary).hasStatusOk()
		assertThat(summary).bodyJson().extractingPath("$.data[0].min").isEqualTo(20000)
		assertThat(summary).bodyJson().extractingPath("$.data[0].allSameValue").isEqualTo(true)

		val removed =
			mockMvcTester
				.delete()
				.uri("/v1/alcohols/attributes/${attribute.id}")
				.header("Authorization", "Bearer $accessToken")
				.exchange()
		assertThat(removed).hasStatusOk()
		assertThat(removed).bodyJson().extractingPath("$.data.code").isEqualTo("ALCOHOL_ATTRIBUTE_DELETED")

		assertThat(
			mockMvcTester
				.put()
				.uri("/v1/alcohols/attributes/404")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					mapper.writeValueAsString(
						AdminAlcoholAttributeUpdateRequest(
							AttributeType.NOTE,
							AttributeDatatype.STRING,
							"메모",
							null,
							null
						)
					)
				)
				.header("Authorization", "Bearer $accessToken")
		).hasStatus(HttpStatus.NOT_FOUND)
	}

	@Test
	@DisplayName("일괄 삭제는 지운 행을 돌려주고 집계와 라벨을 조회할 수 있다")
	fun batchDeletesAndReadsAggregates() {
		val first = alcoholTestFactory.persistAlcohol()
		val second = alcoholTestFactory.persistAlcohol()
		val peat = saveAttribute(first, "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "피트")
		saveAttribute(second, "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "피트")
		val price = saveAttribute(first, "price_bottle", AttributeType.PRICE, AttributeDatatype.INT, "10000")

		val deleted =
			mockMvcTester
				.post()
				.uri("/v1/alcohols/attributes/batch/delete")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(mapOf("ids" to listOf(price.id, 999999L))))
				.header("Authorization", "Bearer $accessToken")
				.exchange()
		assertThat(deleted).hasStatusOk()
		assertThat(deleted).bodyJson().extractingPath("$.data.length()").isEqualTo(1)
		assertThat(deleted).bodyJson().extractingPath("$.data[0].id").isEqualTo(price.id.toInt())
		assertThat(deleted).bodyJson().extractingPath("$.data[0].value").isEqualTo("10000")

		val keys =
			mockMvcTester
				.get()
				.uri("/v1/alcohols/attributes/keys")
				.header("Authorization", "Bearer $accessToken")
				.exchange()
		assertThat(keys).hasStatusOk()
		assertThat(keys).bodyJson().extractingPath("$.data[?(@.key == 'keyword')].count").isEqualTo(listOf(2))

		val keywords =
			mockMvcTester
				.get()
				.uri("/v1/alcohols/attributes/keywords")
				.header("Authorization", "Bearer $accessToken")
				.exchange()
		assertThat(keywords).hasStatusOk()
		assertThat(keywords).bodyJson().extractingPath("$.data[0].value").isEqualTo("피트")
		assertThat(keywords).bodyJson().extractingPath("$.data[0].alcoholCount").isEqualTo(2)

		val enums =
			mockMvcTester
				.get()
				.uri("/v1/alcohols/attributes/enums")
				.header("Authorization", "Bearer $accessToken")
				.exchange()
		assertThat(enums).hasStatusOk()
		assertThat(enums).bodyJson().extractingPath("$.data.types[0].code").isEqualTo("KEYWORD")
		assertThat(enums).bodyJson().extractingPath("$.data.types[0].korName").isEqualTo("키워드")
		assertThat(enums).bodyJson().extractingPath("$.data.providers[?(@.code == 'ADMIN')].engName").isEqualTo(listOf("admin"))

		assertThat(peat.id).isNotNull()
	}

	private fun post(
		uri: String,
		body: Any
	) = mockMvcTester
		.post()
		.uri(uri)
		.contentType(MediaType.APPLICATION_JSON)
		.content(mapper.writeValueAsString(body))
		.header("Authorization", "Bearer $accessToken")
		.exchange()

	private fun saveAttribute(
		alcohol: Alcohol,
		key: String,
		type: AttributeType,
		datatype: AttributeDatatype,
		value: String
	): AlcoholAttribute = alcoholAttributeRepository.save(
		AlcoholAttribute.create(alcohol.id, key, type, datatype, value, null, null)
	)
}
