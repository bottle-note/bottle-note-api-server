package app.bottlenote.alcohols.excel

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup
import app.bottlenote.alcohols.constant.AlcoholType
import app.bottlenote.alcohols.domain.Alcohol
import app.bottlenote.alcohols.domain.Distillery
import app.bottlenote.alcohols.domain.Region
import app.bottlenote.alcohols.domain.TastingTag
import app.bottlenote.alcohols.dto.response.CategoryItem
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

		// 카테고리 참조 시드 (실제 API 카테고리 바인딩 경로와 동일)
		alcoholQueryRepository.save(
			Alcohol.builder()
				.korName("__category_seed__")
				.engName("__category_seed__")
				.abv("0%")
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
		@DisplayName("1페이지 사용 안내·참조 시트·마지막 입력 시트와 헤더 스타일을 가진다")
		fun template_hasFixedStructureWithoutImageColumn() {
			val bytes = service.createTemplateWorkbook()

			WorkbookFactory.create(ByteArrayInputStream(bytes)).use { workbook ->
				assertThat(workbook.numberOfSheets).isEqualTo(6)
				assertThat(workbook.getSheetName(0)).isEqualTo(AlcoholExcelSchema.GUIDE_SHEET_NAME)
				assertThat(workbook.getSheetName(1)).isEqualTo(AlcoholExcelSchema.REGION_SHEET_NAME)
				assertThat(workbook.getSheetName(2)).isEqualTo(AlcoholExcelSchema.DISTILLERY_SHEET_NAME)
				assertThat(workbook.getSheetName(3)).isEqualTo(AlcoholExcelSchema.TASTING_TAG_SHEET_NAME)
				assertThat(workbook.getSheetName(4)).isEqualTo(AlcoholExcelSchema.CATEGORY_SHEET_NAME)
				assertThat(workbook.getSheetName(5)).isEqualTo(AlcoholExcelSchema.DATA_SHEET_NAME)
				assertThat(workbook.getSheetName(workbook.activeSheetIndex)).isEqualTo(AlcoholExcelSchema.GUIDE_SHEET_NAME)

				val dataSheet = workbook.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
				val headerRow = dataSheet.getRow(0)
				val descriptionRow = dataSheet.getRow(1)

				assertThat(AlcoholExcelSchema.HEADERS).hasSize(14)
				AlcoholExcelSchema.HEADERS.forEachIndexed { index, expected ->
					val headerCell = headerRow.getCell(index)
					assertThat(headerCell.stringCellValue).isEqualTo(expected)
					assertThat(headerCell.cellStyle.fillForegroundColor).isEqualTo(IndexedColors.DARK_BLUE.index)
					assertThat(workbook.getFontAt(headerCell.cellStyle.fontIndexAsInt).bold).isTrue()
					assertThat(descriptionRow.getCell(index).stringCellValue)
						.isEqualTo(AlcoholExcelSchema.DESCRIPTIONS[index])
				}

				val headers = (0 until headerRow.lastCellNum).map { headerRow.getCell(it).stringCellValue }
				assertThat(headers).doesNotContain("이미지", "imageUrl", "image_url", "이미지 URL")
				assertThat(headers.joinToString()).doesNotContain("imageUrl")

				// 실제 입력 시트 데이터 행은 비어 있어야 한다
				assertThat(dataSheet.getRow(2)).isNull()

				val regionSheet = workbook.getSheet(AlcoholExcelSchema.REGION_SHEET_NAME)
				assertThat(regionSheet.getRow(1).getCell(0).stringCellValue).isEqualTo("스페이사이드")

				val distillerySheet = workbook.getSheet(AlcoholExcelSchema.DISTILLERY_SHEET_NAME)
				assertThat(distillerySheet.getRow(1).getCell(0).stringCellValue).isEqualTo("글렌피딕")

				val tagSheet = workbook.getSheet(AlcoholExcelSchema.TASTING_TAG_SHEET_NAME)
				assertThat(tagSheet.getRow(1).getCell(0).stringCellValue).isEqualTo("오크")

				val categorySheet = workbook.getSheet(AlcoholExcelSchema.CATEGORY_SHEET_NAME)
				assertThat(categorySheet.getRow(0).getCell(0).stringCellValue).isEqualTo("카테고리 그룹")
				assertThat(categorySheet.getRow(1).getCell(0).stringCellValue).isEqualTo("싱글몰트 위스키")
				assertThat(categorySheet.getRow(1).getCell(1).stringCellValue).isEqualTo("싱글 몰트")
				assertThat(categorySheet.getRow(1).getCell(2).stringCellValue).isEqualTo("Single Malt")

				val guideSheet = workbook.getSheet(AlcoholExcelSchema.GUIDE_SHEET_NAME)
				assertThat(guideSheet.getRow(0).getCell(0).stringCellValue).contains("템플릿")

				// 내부 필드명 미노출
				val allText =
					(0 until workbook.numberOfSheets).flatMap { sheetIdx ->
						val sheet = workbook.getSheetAt(sheetIdx)
						(0..sheet.lastRowNum).flatMap { rowIdx ->
							val row = sheet.getRow(rowIdx) ?: return@flatMap emptyList()
							(0 until row.lastCellNum.coerceAtLeast(0)).mapNotNull { col ->
								row.getCell(col)?.toString()
							}
						}
					}.joinToString("\n")
				assertThat(allText).doesNotContain("korName", "engName", "regionId", "distilleryId", "tastingTagIds")

				// 드롭다운 유효성 존재
				val validations = dataSheet.dataValidations
				assertThat(validations).isNotEmpty
				assertThat(
					validations.any {
						it.validationConstraint.validationType == DataValidationConstraint.ValidationType.LIST
					},
				).isTrue()
			}
		}
	}

	@Nested
	@DisplayName("업로드 검증")
	inner class Validate {
		@Test
		@DisplayName("정상 workbook 업로드가 행과 참조값을 파싱해 valid 결과를 반환한다")
		fun validate_whenValidRow_returnsValidResult() {
			val file = workbookAsMultipart { workbook ->
				writeDataRow(
					workbook,
					listOf(
						"글렌피딕 12년",
						"Glenfiddich 12",
						"40%",
						"위스키",
						"싱글 몰트",
						"Single Malt",
						"싱글몰트 위스키",
						"스페이사이드",
						"글렌피딕",
						"12",
						"American Oak",
						"스페이사이드 대표 싱글몰트",
						"700ml",
						"오크|피트",
					),
				)
			}

			val result = service.validate(file)

			assertThat(result.totalRows).isEqualTo(1)
			assertThat(result.validRows).isEqualTo(1)
			assertThat(result.invalidRows).isEqualTo(0)
			assertThat(result.warningRows).isEqualTo(0)
			assertThat(result.rows).hasSize(1)

			val row = result.rows[0]
			assertThat(row.rowNumber).isEqualTo(3)
			assertThat(row.valid).isTrue()
			assertThat(row.errors).isEmpty()
			assertThat(row.warnings).isEmpty()
			assertThat(row.korName).isEqualTo("글렌피딕 12년")
			assertThat(row.engName).isEqualTo("Glenfiddich 12")
			assertThat(row.abv).isEqualTo("40%")
			assertThat(row.type).isEqualTo(AlcoholType.WHISKY.name)
			assertThat(row.categoryGroup).isEqualTo(AlcoholCategoryGroup.SINGLE_MALT.name)
			assertThat(row.regionId).isEqualTo(region.id)
			assertThat(row.distilleryId).isEqualTo(distillery.id)
			assertThat(row.tastingTagIds).containsExactly(tagOak.id, tagPeat.id)
		}

		@Test
		@DisplayName("헤더 변경은 HEADER_MISMATCH 오류를 반환한다")
		fun validate_whenHeaderChanged_returnsHeaderMismatch() {
			val file = workbookAsMultipart { workbook ->
				val sheet = workbook.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
				sheet.getRow(0).getCell(0).setCellValue("이름")
			}

			assertThatThrownBy { service.validate(file) }
				.isInstanceOf(AlcoholException::class.java)
				.extracting("exceptionCode")
				.isEqualTo(AlcoholExceptionCode.EXCEL_HEADER_MISMATCH)
		}

		@Test
		@DisplayName("설명 행 변경은 DESCRIPTION_MISMATCH 오류를 반환한다")
		fun validate_whenDescriptionChanged_returnsDescriptionMismatch() {
			val file = workbookAsMultipart { workbook ->
				val sheet = workbook.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
				sheet.getRow(1).getCell(0).setCellValue("변경된 설명")
			}

			assertThatThrownBy { service.validate(file) }
				.isInstanceOf(AlcoholException::class.java)
				.extracting("exceptionCode")
				.isEqualTo(AlcoholExceptionCode.EXCEL_DESCRIPTION_MISMATCH)
		}

		@Test
		@DisplayName("잘못된 enum 값은 행 오류 INVALID_ENUM_VALUE 를 반환한다")
		fun validate_whenInvalidEnum_returnsRowError() {
			val file = workbookAsMultipart { workbook ->
				writeDataRow(
					workbook,
					listOf(
						"테스트",
						"Test",
						"40%",
						"없는종류",
						"싱글 몰트",
						"Single Malt",
						"싱글몰트 위스키",
						"스페이사이드",
						"글렌피딕",
						"12",
						"Oak",
						"desc",
						"700ml",
						"오크",
					),
				)
			}

			val result = service.validate(file)
			assertThat(result.invalidRows).isEqualTo(1)
			assertThat(result.rows[0].valid).isFalse()
			assertThat(result.rows[0].errors.map { it.code }).contains("INVALID_ENUM_VALUE")
		}

		@Test
		@DisplayName("없는 지역·증류소·태그는 안정적인 오류 code를 반환한다")
		fun validate_whenUnknownReference_returnsNotFoundCodes() {
			val file = workbookAsMultipart { workbook ->
				writeDataRow(
					workbook,
					listOf(
						"테스트",
						"Test",
						"40%",
						"위스키",
						"싱글 몰트",
						"Single Malt",
						"싱글몰트 위스키",
						"없는지역",
						"없는증류소",
						"12",
						"Oak",
						"desc",
						"700ml",
						"없는태그",
					),
				)
			}

			val result = service.validate(file)
			val codes = result.rows[0].errors.map { it.code }
			assertThat(codes).contains("REGION_NOT_FOUND", "DISTILLERY_NOT_FOUND", "TASTING_TAG_NOT_FOUND")
		}

		@Test
		@DisplayName("수식 셀은 FORMULA_NOT_ALLOWED 오류다")
		fun validate_whenFormulaCell_returnsFormulaNotAllowed() {
			val file = workbookAsMultipart { workbook ->
				writeDataRow(
					workbook,
					listOf(
						"테스트",
						"Test",
						"40%",
						"위스키",
						"싱글 몰트",
						"Single Malt",
						"싱글몰트 위스키",
						"스페이사이드",
						"글렌피딕",
						"12",
						"Oak",
						"desc",
						"700ml",
						"오크",
					),
				)
				val sheet = workbook.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
				val cell = sheet.getRow(2).getCell(0)
				cell.cellFormula = "A1"
				assertThat(cell.cellType).isEqualTo(CellType.FORMULA)
			}

			assertThatThrownBy { service.validate(file) }
				.isInstanceOf(AlcoholException::class.java)
				.extracting("exceptionCode")
				.isEqualTo(AlcoholExceptionCode.EXCEL_FORMULA_NOT_ALLOWED)
		}

		@Test
		@DisplayName("행 제한 초과는 ROW_LIMIT_EXCEEDED 오류다")
		fun validate_whenTooManyRows_returnsRowLimitExceeded() {
			val file = workbookAsMultipart { workbook ->
				repeat(AlcoholExcelSchema.MAX_DATA_ROWS + 1) { idx ->
					writeDataRow(
						workbook,
						listOf(
							"이름$idx",
							"Name$idx",
							"40%",
							"위스키",
							"싱글 몰트",
							"Single Malt",
							"싱글몰트 위스키",
							"스페이사이드",
							"글렌피딕",
							"12",
							"Oak",
							"desc",
							"700ml",
							"오크",
						),
						rowIndex = 2 + idx,
					)
				}
			}

			assertThatThrownBy { service.validate(file) }
				.isInstanceOf(AlcoholException::class.java)
				.extracting("exceptionCode")
				.isEqualTo(AlcoholExceptionCode.EXCEL_ROW_LIMIT_EXCEEDED)
		}

		@Test
		@DisplayName("파일 내부 중복은 오류, 기존 DB 강한 중복은 warning 이다")
		fun validate_duplicateInFileIsError_andDbMatchIsWarning() {
			// 카테고리 시드 제거 후 실제 중복 후보만 남긴다
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

			val file =
				workbookAsMultipart { workbook ->
					val values =
						listOf(
							"글렌피딕 12년",
							"Glenfiddich 12",
							"40%",
							"위스키",
							"싱글 몰트",
							"Single Malt",
							"싱글몰트 위스키",
							"스페이사이드",
							"글렌피딕",
							"12",
							"Oak",
							"desc",
							"700ml",
							"오크",
						)
					writeDataRow(workbook, values, rowIndex = 2)
					writeDataRow(workbook, values, rowIndex = 3)
				}

			val result = service.validate(file)
			assertThat(result.totalRows).isEqualTo(2)
			assertThat(result.rows).allMatch { row -> row.errors.any { it.code == "DUPLICATE_IN_FILE" } }
			assertThat(result.rows).allMatch { row -> row.warnings.any { it.code == "DUPLICATE_CANDIDATE" } }
			assertThat(result.rows[0].candidateAlcoholIds).isNotEmpty
			assertThat(result.rows[0].warnings.map { it.code }).contains("DUPLICATE_CANDIDATE")
		}

		@Test
		@DisplayName("완전히 빈 데이터 행은 무시한다")
		fun validate_skipsCompletelyBlankRows() {
			val file = workbookAsMultipart { workbook ->
				writeDataRow(workbook, List(14) { "" }, rowIndex = 2)
				writeDataRow(
					workbook,
					listOf(
						"유효행",
						"Valid",
						"40%",
						"위스키",
						"싱글 몰트",
						"Single Malt",
						"싱글몰트 위스키",
						"스페이사이드",
						"글렌피딕",
						"12",
						"Oak",
						"desc",
						"700ml",
						"오크",
					),
					rowIndex = 3,
				)
			}

			val result = service.validate(file)
			assertThat(result.totalRows).isEqualTo(1)
			assertThat(result.validRows).isEqualTo(1)
		}

		@Test
		@DisplayName("없는 카테고리 조합은 CATEGORY_NOT_FOUND 오류다")
		fun validate_whenUnknownCategory_returnsCategoryNotFound() {
			val file =
				workbookAsMultipart { workbook ->
					writeDataRow(
						workbook,
						listOf(
							"테스트",
							"Test",
							"40%",
							"위스키",
							"없는카테고리",
							"Unknown",
							"싱글몰트 위스키",
							"스페이사이드",
							"글렌피딕",
							"12",
							"Oak",
							"desc",
							"700ml",
							"오크",
						),
					)
				}

			val result = service.validate(file)
			assertThat(result.rows[0].errors.map { it.code }).contains("CATEGORY_NOT_FOUND")
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

		@Test
		@DisplayName("파일 크기 초과는 FILE_TOO_LARGE 이다")
		fun validate_whenFileTooLarge_returnsFileTooLarge() {
			val oversized = ByteArray(AlcoholExcelSchema.MAX_FILE_BYTES.toInt() + 1)
			val file = MockMultipartFile("file", "alcohol.xlsx", AlcoholExcelSchema.XLSX_CONTENT_TYPE, oversized)

			assertThatThrownBy { service.validate(file) }
				.isInstanceOf(AlcoholException::class.java)
				.extracting("exceptionCode")
				.isEqualTo(AlcoholExceptionCode.EXCEL_FILE_TOO_LARGE)
		}

		@Test
		@DisplayName("중복 테이스팅 태그는 DUPLICATE_TASTING_TAG 오류다")
		fun validate_whenDuplicateTags_returnsDuplicateTastingTag() {
			val file = workbookAsMultipart { workbook ->
				writeDataRow(
					workbook,
					listOf(
						"테스트",
						"Test",
						"40%",
						"위스키",
						"싱글 몰트",
						"Single Malt",
						"싱글몰트 위스키",
						"스페이사이드",
						"글렌피딕",
						"12",
						"Oak",
						"desc",
						"700ml",
						"오크|오크",
					),
				)
			}

			val result = service.validate(file)
			assertThat(result.rows[0].errors.map { it.code }).contains("DUPLICATE_TASTING_TAG")
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
