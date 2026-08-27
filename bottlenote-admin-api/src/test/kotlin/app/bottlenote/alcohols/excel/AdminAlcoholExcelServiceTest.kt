package app.bottlenote.alcohols.excel

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup
import app.bottlenote.alcohols.constant.AlcoholType
import app.bottlenote.alcohols.domain.Alcohol
import app.bottlenote.alcohols.domain.Distillery
import app.bottlenote.alcohols.domain.Region
import app.bottlenote.alcohols.domain.TastingTag
import app.bottlenote.alcohols.exception.AlcoholException
import app.bottlenote.alcohols.exception.AlcoholExceptionCode
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository
import app.bottlenote.alcohols.fixture.InMemoryDistilleryRepository
import app.bottlenote.alcohols.fixture.InMemoryRegionRepository
import app.bottlenote.alcohols.fixture.InMemoryTastingTagRepository
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataValidationConstraint
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.util.ReflectionTestUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@Tag("unit")
@DisplayName("AdminAlcoholExcelService 단위 테스트")
class AdminAlcoholExcelServiceTest {
	private lateinit var regionRepository: InMemoryRegionRepository
	private lateinit var distilleryRepository: InMemoryDistilleryRepository
	private lateinit var tastingTagRepository: InMemoryTastingTagRepository
	private lateinit var alcoholQueryRepository: InMemoryAlcoholQueryRepository
	private lateinit var service: AdminAlcoholExcelService

	private lateinit var region: Region
	private lateinit var distillery: Distillery
	private lateinit var tagOak: TastingTag
	private lateinit var tagPeat: TastingTag

	@BeforeEach
	fun setUp() {
		regionRepository = InMemoryRegionRepository()
		distilleryRepository = InMemoryDistilleryRepository()
		tastingTagRepository = InMemoryTastingTagRepository()
		alcoholQueryRepository = InMemoryAlcoholQueryRepository()
		service =
			AdminAlcoholExcelServiceImpl(
				regionRepository,
				distilleryRepository,
				tastingTagRepository,
				alcoholQueryRepository,
			)

		region =
			regionRepository.save(
				Region.builder().korName("스페이사이드").engName("Speyside").sortOrder(1).build(),
			)
		distillery =
			distilleryRepository.save(
				Distillery.builder().korName("글렌피딕").engName("Glenfiddich").sortOrder(1).build(),
			)
		tagOak = tastingTagRepository.save(TastingTag.builder().korName("오크").engName("Oak").build())
		tagPeat = tastingTagRepository.save(TastingTag.builder().korName("피트").engName("Peat").build())

		alcoholQueryRepository.save(
			Alcohol.builder()
				.korName("__category_seed__")
				.engName("__category_seed__")
				.abv("0")
				.type(AlcoholType.WHISKY)
				.korCategory("싱글 몰트")
				.engCategory("Single Malt")
				.categoryGroup(AlcoholCategoryGroup.SINGLE_MALT)
				.region(region)
				.distillery(distillery)
				.age("-")
				.cask("-")
				.description("category seed")
				.volume("-")
				.build(),
		)
	}

	@Nested
	@DisplayName("템플릿 생성")
	inner class Template {
		@Test
		@DisplayName("1페이지 사용 안내·ID 참조 시트·마지막 입력 시트와 헤더 스타일을 가진다")
		fun template_hasFixedStructureWithoutImageColumn() {
			val bytes = service.createTemplateWorkbook()

			WorkbookFactory.create(ByteArrayInputStream(bytes)).use { workbook ->
				assertThat(workbook.numberOfSheets).isEqualTo(6)
				assertThat(workbook.getSheetName(0)).isEqualTo(AlcoholExcelSchema.GUIDE_SHEET_NAME)
				assertThat(workbook.getSheetName(5)).isEqualTo(AlcoholExcelSchema.DATA_SHEET_NAME)

				val dataSheet = workbook.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
				val headerRow = dataSheet.getRow(0)
				assertThat(AlcoholExcelSchema.HEADERS).contains("지역 ID", "증류소 ID", "카테고리 ID", "테이스팅 태그 ID", "설명")
				AlcoholExcelSchema.HEADERS.forEachIndexed { index, expected ->
					val headerCell = headerRow.getCell(index)
					assertThat(headerCell.stringCellValue).isEqualTo(expected)
					assertThat(headerCell.cellStyle.fillForegroundColor).isEqualTo(IndexedColors.DARK_BLUE.index)
					assertThat(workbook.getFontAt(headerCell.cellStyle.fontIndexAsInt).bold).isTrue()
				}
				assertThat(dataSheet.getRow(2)).isNull()

				val regionSheet = workbook.getSheet(AlcoholExcelSchema.REGION_SHEET_NAME)
				assertThat(regionSheet.getRow(0).getCell(0).stringCellValue).isEqualTo("ID")
				assertThat(regionSheet.getRow(1).getCell(0).stringCellValue).isEqualTo(region.id.toString())

				val guideSheet = workbook.getSheet(AlcoholExcelSchema.GUIDE_SHEET_NAME)
				val guideText =
					(0..guideSheet.lastRowNum).joinToString("\n") { idx ->
						guideSheet.getRow(idx)?.getCell(0)?.toString().orEmpty()
					}
				assertThat(guideText).contains("오류 코드")
				assertThat(guideText).contains("DUPLICATE_CANDIDATE")
				assertThat(guideText).contains("이미 등록된 위스키입니다")
			}
		}
	}

	@Nested
	@DisplayName("업로드 검증")
	inner class Validate {
		@Test
		@DisplayName("정상 workbook이 ID/숫자 규칙으로 valid 결과를 반환한다")
		fun validate_whenValidRow_returnsValidResult() {
			val file =
				workbookAsMultipart { workbook ->
					writeDataRow(
						workbook,
						listOf(
							"글렌피딕 12년",
							"Glenfiddich 12",
							"40.00",
							"위스키",
							"SINGLE_MALT|싱글 몰트|Single Malt",
							"싱글몰트 위스키",
							region.id.toString(),
							distillery.id.toString(),
							"12",
							"American Oak",
							"스페이사이드 대표 싱글몰트",
							"700.00",
							"${tagOak.id}|${tagPeat.id}",
						),
					)
				}

			val result = service.validate(file)
			assertThat(result.validRows).isEqualTo(1)
			val row = result.rows[0]
			assertThat(row.valid).isTrue()
			assertThat(row.abv).isEqualTo("40%")
			assertThat(row.volume).isEqualTo("700ml")
			assertThat(row.regionId).isEqualTo(region.id)
			assertThat(row.distilleryId).isEqualTo(distillery.id)
			assertThat(row.tastingTagIds).containsExactly(tagOak.id, tagPeat.id)
			assertThat(row.description).isEqualTo("스페이사이드 대표 싱글몰트")
			assertThat(row.korCategory).isEqualTo("싱글 몰트")
			assertThat(row.engCategory).isEqualTo("Single Malt")
		}

		@Test
		@DisplayName("도수/용량에 기호가 있으면 INVALID_NUMBER 다")
		fun validate_whenAbvHasSymbol_returnsInvalidNumber() {
			val file =
				workbookAsMultipart { workbook ->
					writeDataRow(
						workbook,
						listOf(
							"테스트",
							"Test",
							"40%",
							"위스키",
							"SINGLE_MALT|싱글 몰트|Single Malt",
							"싱글몰트 위스키",
							region.id.toString(),
							distillery.id.toString(),
							"12",
							"Oak",
							"desc",
							"700ml",
							tagOak.id.toString(),
						),
					)
				}

			val result = service.validate(file)
			assertThat(result.rows[0].errors.map { it.code }).contains("INVALID_NUMBER")
		}

		@Test
		@DisplayName("없는 지역 ID는 REGION_NOT_FOUND 다")
		fun validate_whenUnknownRegionId_returnsNotFound() {
			val file =
				workbookAsMultipart { workbook ->
					writeDataRow(
						workbook,
						listOf(
							"테스트",
							"Test",
							"40.00",
							"위스키",
							"SINGLE_MALT|싱글 몰트|Single Malt",
							"싱글몰트 위스키",
							"999999",
							distillery.id.toString(),
							"12",
							"Oak",
							"desc",
							"700.00",
							tagOak.id.toString(),
						),
					)
				}

			val result = service.validate(file)
			assertThat(result.rows[0].errors.map { it.code }).contains("REGION_NOT_FOUND")
		}

		@Test
		@DisplayName("파일 내부 중복은 오류, 기존 DB 강한 중복은 warning 이다")
		fun validate_duplicateInFileIsError_andDbMatchIsWarning() {
			alcoholQueryRepository.findAll().toList().forEach { alcohol ->
				if (alcohol.korName == "__category_seed__") {
					ReflectionTestUtils.setField(alcohol, "deletedAt", java.time.LocalDateTime.now())
				}
			}
			alcoholQueryRepository.save(
				Alcohol.builder()
					.korName("글렌피딕 12년")
					.engName("Glenfiddich 12")
					.abv("40%")
					.type(AlcoholType.WHISKY)
					.korCategory("싱글 몰트")
					.engCategory("Single Malt")
					.categoryGroup(AlcoholCategoryGroup.SINGLE_MALT)
					.region(region)
					.distillery(distillery)
					.age("12")
					.cask("Oak")
					.volume("700ml")
					.description("existing")
					.build(),
			)

			val values =
				listOf(
					"글렌피딕 12년",
					"Glenfiddich 12",
					"40.00",
					"위스키",
					"SINGLE_MALT|싱글 몰트|Single Malt",
					"싱글몰트 위스키",
					region.id.toString(),
					distillery.id.toString(),
					"12",
					"Oak",
					"desc",
					"700.00",
					tagOak.id.toString(),
				)
			val file =
				workbookAsMultipart { workbook ->
					writeDataRow(workbook, values, rowIndex = 2)
					writeDataRow(workbook, values, rowIndex = 3)
				}

			val result = service.validate(file)
			assertThat(result.rows).allMatch { row -> row.errors.any { it.code == "DUPLICATE_IN_FILE" } }
			assertThat(result.rows).allMatch { row -> row.warnings.any { it.code == "DUPLICATE_CANDIDATE" } }
			assertThat(result.rows[0].warnings[0].message).contains("이미 등록된 위스키입니다")
		}

		@Test
		@DisplayName("40과 40.00은 파일 내부 중복으로 본다")
		fun validate_whenAbvScaleDiffers_isDuplicateInFile() {
			val file =
				workbookAsMultipart { workbook ->
					writeDataRow(
						workbook,
						listOf(
							"글렌피딕 12년",
							"Glenfiddich 12",
							"40",
							"위스키",
							"SINGLE_MALT|싱글 몰트|Single Malt",
							"싱글몰트 위스키",
							region.id.toString(),
							distillery.id.toString(),
							"12",
							"Oak",
							"desc",
							"700",
							tagOak.id.toString(),
						),
						rowIndex = 2,
					)
					writeDataRow(
						workbook,
						listOf(
							"글렌피딕 12년",
							"Glenfiddich 12",
							"40.00",
							"위스키",
							"SINGLE_MALT|싱글 몰트|Single Malt",
							"싱글몰트 위스키",
							region.id.toString(),
							distillery.id.toString(),
							"12",
							"Oak",
							"desc",
							"700.00",
							tagOak.id.toString(),
						),
						rowIndex = 3,
					)
				}

			val result = service.validate(file)
			assertThat(result.rows).allMatch { row -> row.errors.any { it.code == "DUPLICATE_IN_FILE" } }
		}

		@Test
		@DisplayName("xlsx 가 아니면 INVALID_FILE_TYPE 이다")
		fun validate_whenNotXlsx_returnsInvalidFileType() {
			val file = MockMultipartFile("file", "data.csv", "text/csv", "a,b,c".toByteArray())
			assertThatThrownBy { service.validate(file) }
				.isInstanceOf(AlcoholException::class.java)
				.extracting("exceptionCode")
				.isEqualTo(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE)
		}
	}

	private fun workbookAsMultipart(mutate: (XSSFWorkbook) -> Unit): MockMultipartFile {
		val templateBytes = service.createTemplateWorkbook()
		val output = ByteArrayOutputStream()
		WorkbookFactory.create(ByteArrayInputStream(templateBytes)).use { workbook ->
			val xssf = workbook as XSSFWorkbook
			mutate(xssf)
			xssf.write(output)
		}
		return MockMultipartFile(
			"file",
			"alcohol-import.xlsx",
			AlcoholExcelSchema.XLSX_CONTENT_TYPE,
			output.toByteArray(),
		)
	}

	private fun writeDataRow(
		workbook: XSSFWorkbook,
		values: List<String>,
		rowIndex: Int = 2,
	) {
		val sheet = workbook.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
		val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
		values.forEachIndexed { index, value ->
			val cell = row.getCell(index) ?: row.createCell(index)
			cell.setCellValue(value)
		}
	}
}
