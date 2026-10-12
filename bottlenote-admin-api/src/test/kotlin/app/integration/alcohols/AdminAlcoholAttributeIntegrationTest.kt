package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.constant.AttributeDatatype
import app.bottlenote.alcohols.constant.AttributeType
import app.bottlenote.alcohols.domain.Alcohol
import app.bottlenote.alcohols.domain.AlcoholAttribute
import app.bottlenote.alcohols.domain.AlcoholAttributeRepository
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeBatchDeleteRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeCreateRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholAttributeUpdateRequest
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeCreateResponse
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeEnumsResponse
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeItem
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeKeyItem
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeKeywordItem
import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeSummaryItem
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.global.dto.response.AdminResultResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.math.BigDecimal
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

		val result = getAs(accessToken, "/v1/alcohols/attributes?alcoholIds=${first.id},${second.id}")

		assertThat(result).hasStatusOk()
		val items = extractData(result, Array<AdminAlcoholAttributeItem>::class.java)
		assertThat(items).hasSize(2)
		assertThat(items[0].key).isEqualTo("keyword")
		assertThat(items[1].value).isEqualTo("58000")
	}

	@Test
	@DisplayName("위스키 상세 경로는 그대로 위스키 조회다")
	fun alcoholDetailRouteStaysOnAlcoholController() {
		val alcohol = alcoholTestFactory.persistAlcohol()

		val result = getAs(accessToken, "/v1/alcohols/${alcohol.id}")

		assertThat(result).hasStatusOk()
		val detail = extractData(result, Map::class.java)
		assertThat(detail["korName"]).isEqualTo(alcohol.korName)
	}

	@Test
	@DisplayName("alcoholIds가 없으면 400이다")
	fun rejectsMissingAlcoholIds() {
		assertThat(getAs(accessToken, "/v1/alcohols/attributes")).hasStatus(HttpStatus.BAD_REQUEST)
	}

	@Test
	@DisplayName("alcoholIds가 숫자가 아니면 400이다")
	fun rejectsNonNumericAlcoholIds() {
		assertThat(getAs(accessToken, "/v1/alcohols/attributes?alcoholIds=abc"))
			.hasStatus(HttpStatus.BAD_REQUEST)
	}

	@Test
	@DisplayName("인증 없이 조회하면 403이다")
	fun rejectsUnauthenticatedRequest() {
		assertThat(mockMvcTester.get().uri("/v1/alcohols/attributes/enums")).hasStatus(HttpStatus.FORBIDDEN)
	}

	@Test
	@DisplayName("속성을 추가하고 같은 값과 대소문자만 다른 값은 건너뛴다")
	fun createsAndSkipsDuplicate() {
		val alcohol = alcoholTestFactory.persistAlcohol()
		saveAttribute(alcohol, "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "Smoky")
		val request =
			AdminAlcoholAttributeCreateRequest(
				listOf(alcohol.id),
				"keyword",
				AttributeType.KEYWORD,
				AttributeDatatype.STRING,
				"smoky",
				"추천 필터",
				LocalDateTime.of(2026, 10, 12, 9, 0)
			)

		val skipped = postAs(accessToken, "/v1/alcohols/attributes", request)

		assertThat(skipped).hasStatusOk()
		val body = extractData(skipped, AdminAlcoholAttributeCreateResponse::class.java)
		assertThat(body.added).isEqualTo(0)
		assertThat(body.skipped).isEqualTo(1)

		val created =
			postAs(
				accessToken,
				"/v1/alcohols/attributes",
				AdminAlcoholAttributeCreateRequest(
					listOf(alcohol.id),
					"keyword",
					AttributeType.KEYWORD,
					AttributeDatatype.STRING,
					"피트",
					"추천 필터",
					LocalDateTime.of(2026, 10, 12, 9, 0)
				)
			)
		assertThat(created).hasStatusOk()
		assertThat(extractData(created, AdminAlcoholAttributeCreateResponse::class.java).added).isEqualTo(1)
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

		assertThat(postAs(accessToken, "/v1/alcohols/attributes", request)).hasStatus(HttpStatus.NOT_FOUND)
	}

	@Test
	@DisplayName("같은 항목과 값으로 고치면 409이고 한 건을 지우면 행이 없다")
	fun updatesAndDeletesAttributes() {
		val alcohol = alcoholTestFactory.persistAlcohol()
		val kept =
			saveAttribute(alcohol, "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "Smoky")
		val attribute =
			saveAttribute(alcohol, "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "Peat")

		val conflict =
			putAs(
				accessToken,
				"/v1/alcohols/attributes/${attribute.id}",
				AdminAlcoholAttributeUpdateRequest(
					AttributeType.KEYWORD,
					AttributeDatatype.STRING,
					"smoky",
					null,
					null
				)
			)
		assertThat(conflict).hasStatus(HttpStatus.CONFLICT)
		assertThat(alcoholAttributeRepository.findById(attribute.id).orElseThrow().attributeValue).isEqualTo("Peat")

		val price =
			saveAttribute(alcohol, "price_bottle", AttributeType.PRICE, AttributeDatatype.INT, "10000")
		val updated =
			putAs(
				accessToken,
				"/v1/alcohols/attributes/${price.id}",
				AdminAlcoholAttributeUpdateRequest(
					AttributeType.PRICE,
					AttributeDatatype.INT,
					"20000",
					"시세",
					null
				)
			)
		assertThat(updated).hasStatusOk()
		assertThat(extractData(updated, AdminResultResponse::class.java).code)
			.isEqualTo("ALCOHOL_ATTRIBUTE_UPDATED")

		val summary = getAs(accessToken, "/v1/alcohols/attributes/summary?alcoholIds=${alcohol.id}")
		assertThat(summary).hasStatusOk()
		val priceSummary =
			extractData(summary, Array<AdminAlcoholAttributeSummaryItem>::class.java)
				.first { it.key == "price_bottle" }
		assertThat(priceSummary.min).isEqualByComparingTo(BigDecimal("20000"))
		assertThat(priceSummary.allSameValue).isTrue()
		assertThat(priceSummary.attributeIds).contains(price.id)
		assertThat(priceSummary.registeredCount).isEqualTo(1)

		val removed = deleteAs(accessToken, "/v1/alcohols/attributes/${price.id}")
		assertThat(removed).hasStatusOk()
		assertThat(extractData(removed, AdminResultResponse::class.java).code)
			.isEqualTo("ALCOHOL_ATTRIBUTE_DELETED")
		assertThat(alcoholAttributeRepository.findById(price.id)).isEmpty()
		assertThat(alcoholAttributeRepository.findById(kept.id)).isPresent()

		val missing =
			putAs(
				accessToken,
				"/v1/alcohols/attributes/404",
				AdminAlcoholAttributeUpdateRequest(
					AttributeType.NOTE,
					AttributeDatatype.STRING,
					"메모",
					null,
					null
				)
			)
		assertThat(missing).hasStatus(HttpStatus.NOT_FOUND)
	}

	@Test
	@DisplayName("일괄 삭제는 지운 행을 돌려주고 집계와 라벨을 조회할 수 있다")
	fun batchDeletesAndReadsAggregates() {
		val first = alcoholTestFactory.persistAlcohol()
		val second = alcoholTestFactory.persistAlcohol()
		saveAttribute(first, "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "피트")
		saveAttribute(second, "keyword", AttributeType.KEYWORD, AttributeDatatype.STRING, "피트")
		val price = saveAttribute(first, "price_bottle", AttributeType.PRICE, AttributeDatatype.INT, "10000")

		val deleted =
			postAs(
				accessToken,
				"/v1/alcohols/attributes/batch/delete",
				AdminAlcoholAttributeBatchDeleteRequest(listOf(price.id, 999999L))
			)
		assertThat(deleted).hasStatusOk()
		val deletedRows = extractData(deleted, Array<AdminAlcoholAttributeItem>::class.java)
		assertThat(deletedRows).hasSize(1)
		assertThat(deletedRows[0].id).isEqualTo(price.id)
		assertThat(deletedRows[0].value).isEqualTo("10000")
		assertThat(alcoholAttributeRepository.findById(price.id)).isEmpty()

		val keys = getAs(accessToken, "/v1/alcohols/attributes/keys")
		assertThat(keys).hasStatusOk()
		val keyItems = extractData(keys, Array<AdminAlcoholAttributeKeyItem>::class.java)
		val keyword = keyItems.first { it.key == "keyword" }
		assertThat(keyword.count).isEqualTo(2)
		assertThat(keyItems.none { it.key == "price_bottle" }).isTrue()

		val keywords = getAs(accessToken, "/v1/alcohols/attributes/keywords")
		assertThat(keywords).hasStatusOk()
		val keywordItems = extractData(keywords, Array<AdminAlcoholAttributeKeywordItem>::class.java)
		assertThat(keywordItems[0].value).isEqualTo("피트")
		assertThat(keywordItems[0].alcoholCount).isEqualTo(2)

		val enums = getAs(accessToken, "/v1/alcohols/attributes/enums")
		assertThat(enums).hasStatusOk()
		val labels = extractData(enums, AdminAlcoholAttributeEnumsResponse::class.java)
		assertThat(labels.types[0].code).isEqualTo("KEYWORD")
		assertThat(labels.types[0].korName).isEqualTo("키워드")
		assertThat(labels.providers.first { it.code == "ADMIN" }.engName).isEqualTo("admin")
	}

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
