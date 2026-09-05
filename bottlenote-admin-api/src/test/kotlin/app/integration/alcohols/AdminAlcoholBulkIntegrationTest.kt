package app.integration.alcohols

import app.IntegrationTestSupport
import app.bottlenote.alcohols.constant.AlcoholCategoryGroup
import app.bottlenote.alcohols.constant.AlcoholType
import app.bottlenote.alcohols.domain.Alcohol
import app.bottlenote.alcohols.domain.Distillery
import app.bottlenote.alcohols.domain.Region
import app.bottlenote.alcohols.excel.AlcoholExcelSchema
import app.bottlenote.alcohols.fixture.AlcoholTestFactory
import app.bottlenote.alcohols.fixture.TastingTagTestFactory
import app.bottlenote.common.file.event.payload.ImageResourceActivatedEvent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.PayloadApplicationEvent
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.assertj.MvcTestResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicInteger

@Tag("admin_integration")
@DisplayName("[integration] Admin Alcohol Bulk API 통합 테스트")
class AdminAlcoholBulkIntegrationTest : IntegrationTestSupport() {
	@Autowired
	private lateinit var alcoholTestFactory: AlcoholTestFactory

	@Autowired
	private lateinit var tastingTagTestFactory: TastingTagTestFactory

	@Autowired
	private lateinit var jdbcTemplate: JdbcTemplate

	@Autowired
	private lateinit var applicationContext: ConfigurableApplicationContext

	private lateinit var accessToken: String

	private var adminId: Long = 0

	@BeforeEach
	fun setUp() {
		val admin = adminUserTestFactory.persistRootAdmin()
		adminId = admin.id
		accessToken = getAccessToken(admin)
	}

	@Nested
	@DisplayName("엑셀 검증 결과를 등록할 때")
	inner class ExcelRoundTrip {
		@Test
		@DisplayName("다운로드한 템플릿의 normalized 행을 벌크 등록하면 실제 필드와 태그가 저장된다")
		fun downloadedTemplateCanBeValidatedAndSaved() {
			val region = alcoholTestFactory.persistRegion()
			val distillery = alcoholTestFactory.persistDistillery()
			val tag1 = tastingTagTestFactory.persistTastingTag("바닐라", "Vanilla")
			val tag2 = tastingTagTestFactory.persistTastingTag("피트", "Peat")
			alcoholTestFactory.persistAlcoholWithCategory("싱글 몰트", "Single Malt")
			val template = downloadTemplate()
			val workbookBytes =
				fillTemplate(
					template,
					listOf(
						"벌크 엑셀 위스키",
						"Bulk Excel Whisky",
						"46.50",
						"위스키",
						"SINGLE_MALT|싱글 몰트|Single Malt",
						"싱글몰트 위스키",
						region.id.toString(),
						distillery.id.toString(),
						"15",
						"Sherry Oak",
						"엑셀 왕복 등록 설명",
						"750.00",
						"${tag1.id}|${tag2.id}"
					)
				)
			val validateResult = validateExcel(workbookBytes)

			assertThat(validateResult).hasStatusOk()
			val validation = responseData(validateResult)
			assertThat(validation.path("validRows").asInt()).isEqualTo(1)
			val normalized = validation.at("/rows/0/normalized")
			assertThat(normalized.isObject).isTrue()
			assertThat(normalized.path("clientRowId").asText()).isEqualTo("3")

			val createResult = postBulk(listOf(normalized))

			assertThat(createResult).hasStatusOk()
			val created = responseData(createResult)
			assertThat(created.path("createdRows").asInt()).isEqualTo(1)
			assertThat(created.at("/rows/0/clientRowId").asText()).isEqualTo("3")
			val alcoholId = created.at("/rows/0/alcoholId").asLong()
			assertStoredAlcohol(
				alcoholId = alcoholId,
				korName = "벌크 엑셀 위스키",
				engName = "Bulk Excel Whisky",
				abv = "46.5%",
				regionId = region.id,
				distilleryId = distillery.id,
				age = "15",
				cask = "Sherry Oak",
				description = "엑셀 왕복 등록 설명",
				volume = "750ml",
				tastingTagIds = listOf(tag1.id, tag2.id)
			)
		}

		@Test
		@DisplayName("엑셀 업로드 파일이 누락되면 400을 반환한다")
		fun missingExcelUploadReturnsBadRequest() {
			val result =
				mockMvcTester.perform(
					multipart("/v1/alcohols/excel/validate")
						.header("Authorization", "Bearer $accessToken")
				)

			assertThat(result).hasStatus(400)
			assertThat(responseJson(result).path("success").asBoolean()).isFalse()
		}
	}

	@Nested
	@DisplayName("JSON 검증 결과를 등록할 때")
	inner class JsonRoundTrip {
		@Test
		@DisplayName("직접 작성한 JSON의 normalized 행을 벌크 등록하면 정규화된 값이 저장된다")
		fun directJsonCanBeValidatedAndSaved() {
			val region = alcoholTestFactory.persistRegion()
			val distillery = alcoholTestFactory.persistDistillery()
			val tag = tastingTagTestFactory.persistTastingTag("오크", "Oak")
			alcoholTestFactory.persistAlcoholWithCategory("싱글 몰트", "Single Malt")
			val request =
				validRow(
					clientRowId = "json-1",
					korName = "직접 JSON 위스키",
					engName = "Direct JSON Whisky",
					regionId = region.id,
					distilleryId = distillery.id,
					tastingTagIds = listOf(tag.id)
				).apply {
					this["type"] = "위스키"
					this["categoryGroup"] = null
					this["abv"] = "40.00"
					this["volume"] = "70cl"
					this.remove("age")
					this.remove("cask")
					this.remove("description")
				}
			val validateResult = postValidate(listOf(mapper.valueToTree<JsonNode>(request)))

			assertThat(validateResult).hasStatusOk()
			val normalized = responseData(validateResult).at("/rows/0/normalized")
			assertThat(normalized.path("type").asText()).isEqualTo("WHISKY")
			assertThat(normalized.path("categoryGroup").asText()).isEqualTo("SINGLE_MALT")
			assertThat(normalized.path("abv").asText()).isEqualTo("40%")
			assertThat(normalized.path("volume").asText()).isEqualTo("700ml")

			val createResult = postBulk(listOf(normalized))

			assertThat(createResult).hasStatusOk()
			val alcoholId = responseData(createResult).at("/rows/0/alcoholId").asLong()
			assertStoredAlcohol(
				alcoholId = alcoholId,
				korName = "직접 JSON 위스키",
				engName = "Direct JSON Whisky",
				abv = "40%",
				regionId = region.id,
				distilleryId = distillery.id,
				age = null,
				cask = null,
				description = null,
				volume = "700ml",
				tastingTagIds = listOf(tag.id)
			)
		}

		@Test
		@DisplayName("검증된 두 번째 행을 변조하면 재검증에서 거절하고 한 건도 저장하지 않는다")
		fun tamperedNormalizedRowDoesNotSaveAnyAlcohol() {
			val region = alcoholTestFactory.persistRegion()
			val distillery = alcoholTestFactory.persistDistillery()
			alcoholTestFactory.persistAlcoholWithCategory("싱글 몰트", "Single Malt")
			val requestRows =
				listOf(
					mapper.valueToTree<JsonNode>(
						validRow("atomic-1", "원자적 등록 하나", "Atomic One", region.id, distillery.id)
					),
					mapper.valueToTree<JsonNode>(
						validRow("atomic-2", "원자적 등록 둘", "Atomic Two", region.id, distillery.id)
					)
				)
			val validateResult = postValidate(requestRows)
			assertThat(validateResult).hasStatusOk()
			val validation = responseData(validateResult)
			assertThat(validation.path("invalidRows").asInt()).isZero()
			val first = validation.at("/rows/0/normalized").deepCopy<ObjectNode>()
			val second = validation.at("/rows/1/normalized").deepCopy<ObjectNode>().put("regionId", UNKNOWN_ID)
			val countBefore = alcoholCount()

			val createResult = postBulk(listOf(first, second))

			assertThat(createResult).hasStatus(400)
			val response = responseJson(createResult)
			assertThat(response.path("success").asBoolean()).isFalse()
			assertThat(response.at("/errors/invalidRows").asInt()).isEqualTo(1)
			assertThat(response.at("/errors/rows/1/errors").map { it.path("code").asText() })
				.contains("INVALID_REFERENCE")
			assertThat(alcoholCount()).isEqualTo(countBefore)
		}

		@Test
		@DisplayName("DB 중복 경고가 있는 서로 다른 행은 모두 새 ID로 등록한다")
		fun duplicateCandidateWarningsStillCreateDistinctAlcohols() {
			val region = alcoholTestFactory.persistRegion()
			val distillery = alcoholTestFactory.persistDistillery()
			val existing1 = persistDuplicateCandidate("중복 후보 하나", "Duplicate Candidate One", region, distillery)
			val existing2 = persistDuplicateCandidate("중복 후보 둘", "Duplicate Candidate Two", region, distillery)
			val requestRows =
				listOf(
					mapper.valueToTree<JsonNode>(
						validRow("warning-1", existing1.korName, existing1.engName, region.id, distillery.id)
					),
					mapper.valueToTree<JsonNode>(
						validRow("warning-2", existing2.korName, existing2.engName, region.id, distillery.id)
					)
				)
			val validateResult = postValidate(requestRows)
			assertThat(validateResult).hasStatusOk()
			val validation = responseData(validateResult)
			assertThat(validation.path("invalidRows").asInt()).isZero()
			assertThat(validation.path("warningRows").asInt()).isEqualTo(2)
			assertThat(validation.path("rows").map { it.path("warnings").map { warning -> warning.path("code").asText() } })
				.allSatisfy { codes -> assertThat(codes).contains("DUPLICATE_DB_CANDIDATE") }
			val normalizedRows = validation.path("rows").map { it.path("normalized") }
			val countBefore = alcoholCount()

			val createResult = postBulk(normalizedRows)

			assertThat(createResult).hasStatusOk()
			val created = responseData(createResult)
			assertThat(created.path("createdRows").asInt()).isEqualTo(2)
			val createdIds = created.path("rows").map { it.path("alcoholId").asLong() }
			assertThat(createdIds).doesNotHaveDuplicates()
			assertThat(createdIds).doesNotContain(existing1.id, existing2.id)
			assertThat(created.path("validation").path("warningRows").asInt()).isEqualTo(2)
			assertThat(alcoholCount()).isEqualTo(countBefore + 2)
		}

		@Test
		@DisplayName("두 번째 이미지 이벤트 발행 중 예외가 발생하면 모든 저장을 롤백한다")
		fun runtimeFailureDuringSecondRowRollsBackAllAlcohols() {
			val region = alcoholTestFactory.persistRegion()
			val distillery = alcoholTestFactory.persistDistillery()
			alcoholTestFactory.persistAlcoholWithCategory("싱글 몰트", "Single Malt")
			val rows =
				listOf(
					validRow("rollback-1", "롤백 등록 하나", "Rollback One", region.id, distillery.id).apply {
						this["imageUrl"] = "https://cdn.bottlenote.com/alcohol/rollback-one.jpg"
					},
					validRow("rollback-2", "롤백 등록 둘", "Rollback Two", region.id, distillery.id).apply {
						this["imageUrl"] = "https://cdn.bottlenote.com/alcohol/rollback-two.jpg"
					}
				)
			val countBefore = alcoholCount()
			val imageEvents = AtomicInteger()
			val listener =
				ApplicationListener<PayloadApplicationEvent<*>> { event ->
					if (event.payload is ImageResourceActivatedEvent && imageEvents.incrementAndGet() == 2) {
						throw IllegalStateException("두 번째 이미지 이벤트 테스트 실패")
					}
				}
			applicationContext.addApplicationListener(listener)

			try {
				val result = postJson("/v1/alcohols/bulk", mapOf("rows" to rows))

				assertThat(result).hasStatus(500)
				assertThat(imageEvents.get()).isEqualTo(2)
				assertThat(alcoholCount()).isEqualTo(countBefore)
			} finally {
				applicationContext.removeApplicationListener(listener)
			}
		}
	}

	@Nested
	@DisplayName("입력 경계를 검증할 때")
	inner class RequestValidation {
		@Test
		@DisplayName("중복 clientRowId는 각 행의 오류로 반환한다")
		fun duplicateClientRowIdsAreRejected() {
			val region = alcoholTestFactory.persistRegion()
			val distillery = alcoholTestFactory.persistDistillery()
			alcoholTestFactory.persistAlcoholWithCategory("싱글 몰트", "Single Malt")
			val rows =
				listOf(
					mapper.valueToTree<JsonNode>(
						validRow("same-row", "중복 행 하나", "Duplicate Row One", region.id, distillery.id)
					),
					mapper.valueToTree<JsonNode>(
						validRow("same-row", "중복 행 둘", "Duplicate Row Two", region.id, distillery.id)
					)
				)

			val result = postValidate(rows)

			assertThat(result).hasStatusOk()
			val validation = responseData(result)
			assertThat(validation.path("invalidRows").asInt()).isEqualTo(2)
			assertThat(validation.path("rows").map { it.path("errors").map { error -> error.path("code").asText() } })
				.allSatisfy { codes -> assertThat(codes).contains("DUPLICATE_CLIENT_ROW_ID") }
		}

		@Test
		@DisplayName("빈 rows 요청은 Bean Validation 오류로 400을 반환한다")
		fun emptyRowsReturnBadRequest() {
			val result = postJson("/v1/alcohols/bulk/validate", mapOf("rows" to emptyList<Any>()))

			assertThat(result).hasStatus(400)
			assertThat(responseJson(result).path("success").asBoolean()).isFalse()
		}

		@Test
		@DisplayName("1,000행을 초과한 rows 요청은 Bean Validation 오류로 400을 반환한다")
		fun moreThanOneThousandRowsReturnBadRequest() {
			val oversizedRows = List(1001) { index -> mapOf("clientRowId" to "row-$index") }

			val result = postJson("/v1/alcohols/bulk/validate", mapOf("rows" to oversizedRows))

			assertThat(result).hasStatus(400)
			assertThat(responseJson(result).path("success").asBoolean()).isFalse()
		}

		@Test
		@DisplayName("잘못된 enum·참조·수량은 행 오류로 반환하고 normalized를 노출하지 않는다")
		fun invalidBusinessFieldsReturnRowErrors() {
			val row =
				validRow("invalid-1", "잘못된 입력", "Invalid Input", UNKNOWN_ID, UNKNOWN_ID).apply {
					this["type"] = "UNKNOWN_TYPE"
					this["categoryGroup"] = "UNKNOWN_GROUP"
					this["abv"] = "101%"
					this["volume"] = "0ml"
					this["tastingTagIds"] = listOf(UNKNOWN_ID)
				}

			val result = postValidate(listOf(mapper.valueToTree<JsonNode>(row)))

			assertThat(result).hasStatusOk()
			val rowResult = responseData(result).at("/rows/0")
			assertThat(rowResult.path("valid").asBoolean()).isFalse()
			assertThat(rowResult.path("normalized").isNull).isTrue()
			assertThat(rowResult.path("errors").map { it.path("code").asText() })
				.contains("INVALID_ENUM", "INVALID_QUANTITY", "INVALID_REFERENCE")
		}

		@ParameterizedTest(name = "{0}에 소수 ID를 보내면 400을 반환한다")
		@ValueSource(strings = ["regionId", "tastingTagIds"])
		@DisplayName("참조 ID에 소수를 보내면 JSON 역직렬화 단계에서 거절한다")
		fun decimalReferenceIdsReturnBadRequest(field: String) {
			val row =
				if (field == "regionId") {
					mapOf(field to 1.5)
				} else {
					mapOf(field to listOf(1.5))
				}

			val result = postJson("/v1/alcohols/bulk/validate", mapOf("rows" to listOf(row)))

			assertThat(result).hasStatus(400)
			assertThat(responseJson(result).path("success").asBoolean()).isFalse()
		}

		@ParameterizedTest(name = "인증 없이 {0}을 호출하면 401을 반환한다")
		@ValueSource(strings = ["/v1/alcohols/bulk/validate", "/v1/alcohols/bulk"])
		@DisplayName("인증이 없으면 벌크 엔드포인트는 401을 반환한다")
		fun unauthenticatedRequestsReturnUnauthorized(path: String) {
			val result =
				mockMvcTester
					.post()
					.uri(path)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"rows\":[]}")
					.exchange()

			assertThat(result).hasStatus(401)
		}
	}

	private fun downloadTemplate(): ByteArray = mockMvcTester
		.get()
		.uri("/v1/alcohols/excel/template")
		.header("Authorization", "Bearer $accessToken")
		.exchange()
		.response.contentAsByteArray

	private fun fillTemplate(
		template: ByteArray,
		values: List<String>
	): ByteArray {
		val output = ByteArrayOutputStream()
		WorkbookFactory.create(ByteArrayInputStream(template)).use { workbook ->
			val sheet = workbook.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
			val row = sheet.createRow(AlcoholExcelSchema.DATA_START_ROW_INDEX)
			values.forEachIndexed { index, value -> row.createCell(index).setCellValue(value) }
			workbook.write(output)
		}
		return output.toByteArray()
	}

	private fun validateExcel(workbookBytes: ByteArray): MvcTestResult {
		val file =
			MockMultipartFile(
				"file",
				"filled-alcohol-import.xlsx",
				AlcoholExcelSchema.XLSX_CONTENT_TYPE,
				workbookBytes
			)
		return mockMvcTester.perform(
			multipart("/v1/alcohols/excel/validate")
				.file(file)
				.header("Authorization", "Bearer $accessToken")
		)
	}

	private fun postValidate(rows: List<JsonNode>): MvcTestResult = postJson("/v1/alcohols/bulk/validate", mapOf("rows" to rows))

	private fun postBulk(rows: List<JsonNode>): MvcTestResult = postJson("/v1/alcohols/bulk", mapOf("rows" to rows))

	private fun postJson(
		path: String,
		body: Any
	): MvcTestResult = mockMvcTester
		.post()
		.uri(path)
		.header("Authorization", "Bearer $accessToken")
		.contentType(MediaType.APPLICATION_JSON)
		.content(mapper.writeValueAsString(body))
		.exchange()

	private fun responseJson(result: MvcTestResult): JsonNode = mapper.readTree(result.response.contentAsByteArray)

	private fun responseData(result: MvcTestResult): JsonNode = responseJson(result).path("data")

	private fun validRow(
		clientRowId: String,
		korName: String,
		engName: String,
		regionId: Long,
		distilleryId: Long,
		tastingTagIds: List<Long> = emptyList()
	): MutableMap<String, Any?> = mutableMapOf(
		"clientRowId" to clientRowId,
		"korName" to korName,
		"engName" to engName,
		"abv" to "40%",
		"type" to AlcoholType.WHISKY.name,
		"korCategory" to "싱글 몰트",
		"engCategory" to "Single Malt",
		"categoryGroup" to AlcoholCategoryGroup.SINGLE_MALT.name,
		"regionId" to regionId,
		"distilleryId" to distilleryId,
		"age" to "12",
		"cask" to "Oak",
		"description" to "벌크 등록 통합 테스트",
		"volume" to "700ml",
		"tastingTagIds" to tastingTagIds
	)

	private fun persistDuplicateCandidate(
		korName: String,
		engName: String,
		region: Region,
		distillery: Distillery
	): Alcohol = alcoholTestFactory.persistAlcohol(
		Alcohol.builder()
			.korName(korName)
			.engName(engName)
			.abv("40%")
			.type(AlcoholType.WHISKY)
			.korCategory("싱글 몰트")
			.engCategory("Single Malt")
			.categoryGroup(AlcoholCategoryGroup.SINGLE_MALT)
			.region(region)
			.distillery(distillery)
			.age("12")
			.cask("Oak")
			.description("기존 중복 후보")
			.volume("700ml")
	)

	private fun assertStoredAlcohol(
		alcoholId: Long,
		korName: String,
		engName: String,
		abv: String,
		regionId: Long,
		distilleryId: Long,
		age: String?,
		cask: String?,
		description: String?,
		volume: String,
		tastingTagIds: List<Long>
	) {
		val stored =
			jdbcTemplate.queryForMap(
				"""
				SELECT id, kor_name, eng_name, abv, type, kor_category, eng_category,
				       category_group, region_id, distillery_id, age, cask, description, volume,
				       create_principal_id, create_principal_type
				FROM alcohols
				WHERE id = ?
				""".trimIndent(),
				alcoholId
			)
		assertThat((stored["id"] as Number).toLong()).isEqualTo(alcoholId)
		assertThat(stored["kor_name"]).isEqualTo(korName)
		assertThat(stored["eng_name"]).isEqualTo(engName)
		assertThat(stored["abv"]).isEqualTo(abv)
		assertThat(stored["type"]).isEqualTo("WHISKY")
		assertThat(stored["kor_category"]).isEqualTo("싱글 몰트")
		assertThat(stored["eng_category"]).isEqualTo("Single Malt")
		assertThat(stored["category_group"]).isEqualTo("SINGLE_MALT")
		assertThat((stored["region_id"] as Number).toLong()).isEqualTo(regionId)
		assertThat((stored["distillery_id"] as Number).toLong()).isEqualTo(distilleryId)
		assertThat(stored["age"]).isEqualTo(age)
		assertThat(stored["cask"]).isEqualTo(cask)
		assertThat(stored["description"]).isEqualTo(description)
		assertThat(stored["volume"]).isEqualTo(volume)
		assertThat((stored["create_principal_id"] as Number).toLong()).isEqualTo(adminId)
		assertThat(stored["create_principal_type"]).isEqualTo("ADMIN")
		assertThat(
			jdbcTemplate.queryForList(
				"SELECT tasting_tag_id FROM alcohols_tasting_tags WHERE alcohol_id = ? ORDER BY tasting_tag_id",
				Long::class.java,
				alcoholId
			)
		).containsExactlyElementsOf(tastingTagIds.sorted())
	}

	private fun alcoholCount(): Long = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM alcohols", Long::class.java) ?: 0L

	companion object {
		private const val UNKNOWN_ID = 999_999_999L
	}
}
