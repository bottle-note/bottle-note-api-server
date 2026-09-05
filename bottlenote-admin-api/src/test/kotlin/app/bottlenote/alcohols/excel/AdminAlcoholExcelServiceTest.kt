package app.bottlenote.alcohols.excel

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup
import app.bottlenote.alcohols.constant.AlcoholType
import app.bottlenote.alcohols.domain.Alcohol
import app.bottlenote.alcohols.domain.Distillery
import app.bottlenote.alcohols.domain.Region
import app.bottlenote.alcohols.domain.TastingTag
import app.bottlenote.alcohols.dto.request.AdminAlcoholBulkRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholBulkRowRequest
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkCreateResponse
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkIssue
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkRowResult
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkValidateResponse
import app.bottlenote.alcohols.exception.AlcoholException
import app.bottlenote.alcohols.exception.AlcoholExceptionCode
import app.bottlenote.alcohols.fixture.InMemoryAlcoholQueryRepository
import app.bottlenote.alcohols.fixture.InMemoryDistilleryRepository
import app.bottlenote.alcohols.fixture.InMemoryRegionRepository
import app.bottlenote.alcohols.fixture.InMemoryTastingTagRepository
import app.bottlenote.alcohols.service.AdminAlcoholBulkService
import org.apache.poi.common.usermodel.HyperlinkType
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
import java.math.BigDecimal
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Tag("unit")
@DisplayName("AdminAlcoholExcelService 단위 테스트")
class AdminAlcoholExcelServiceTest {
	private lateinit var regionRepository: InMemoryRegionRepository
	private lateinit var distilleryRepository: InMemoryDistilleryRepository
	private lateinit var tastingTagRepository: InMemoryTastingTagRepository
	private lateinit var alcoholQueryRepository: InMemoryAlcoholQueryRepository
	private lateinit var bulkService: FakeAdminAlcoholBulkService
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
		bulkService = FakeAdminAlcoholBulkService()
		service =
			AdminAlcoholExcelServiceImpl(
				regionRepository,
				distilleryRepository,
				tastingTagRepository,
				alcoholQueryRepository,
				bulkService
			)

		region =
			regionRepository.save(
				Region.builder().korName("스페이사이드").engName("Speyside").sortOrder(1).build()
			)
		distillery =
			distilleryRepository.save(
				Distillery.builder().korName("글렌피딕").engName("Glenfiddich").sortOrder(1).build()
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
				.build()
		)
	}

	@Nested
	@DisplayName("템플릿 생성")
	inner class Template {
		@Test
		@DisplayName("카테고리 참조 시트는 모든 주류 타입의 유효한 카테고리를 중복 없이 제공한다")
		fun template_includesAllAlcoholTypeCategories() {
			alcoholQueryRepository.save(
				Alcohol.builder()
					.korName("__rum_category_seed__")
					.engName("__rum_category_seed__")
					.abv("40%")
					.type(AlcoholType.RUM)
					.korCategory("럼")
					.engCategory("Rum")
					.categoryGroup(AlcoholCategoryGroup.OTHER)
					.region(region)
					.distillery(distillery)
					.age("-")
					.cask("-")
					.description("rum category seed")
					.volume("700ml")
					.build()
			)

			WorkbookFactory.create(ByteArrayInputStream(service.createTemplateWorkbook())).use { workbook ->
				val categorySheet = workbook.getSheet(AlcoholExcelSchema.CATEGORY_SHEET_NAME)
				val categoryIds =
					(1..categorySheet.lastRowNum).map { rowIndex ->
						categorySheet.getRow(rowIndex).getCell(0).stringCellValue
					}

				assertThat(categoryIds).contains("SINGLE_MALT|싱글 몰트|Single Malt", "OTHER|럼|Rum")
				assertThat(categoryIds).doesNotHaveDuplicates()
			}
		}

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
					(0..guideSheet.lastRowNum)
						.flatMap { rowIndex ->
							val row = guideSheet.getRow(rowIndex) ?: return@flatMap emptyList()
							(0 until row.lastCellNum.coerceAtLeast(0)).map { cellIndex ->
								row.getCell(cellIndex)?.toString().orEmpty()
							}
						}.joinToString("\n")
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
							"${tagOak.id}|${tagPeat.id}"
						)
					)
				}

			val result = service.validate(file)
			assertThat(result.validRows).isEqualTo(1)
			val row = result.rows[0]
			assertThat(row.valid).isTrue()
			assertThat(row.clientRowId).isEqualTo("3")
			assertThat(row.abv).isEqualTo("40%")
			assertThat(row.volume).isEqualTo("700ml")
			assertThat(row.regionId).isEqualTo(region.id)
			assertThat(row.distilleryId).isEqualTo(distillery.id)
			assertThat(row.tastingTagIds).containsExactly(tagOak.id, tagPeat.id)
			assertThat(row.description).isEqualTo("스페이사이드 대표 싱글몰트")
			assertThat(row.korCategory).isEqualTo("싱글 몰트")
			assertThat(row.engCategory).isEqualTo("Single Malt")
			assertThat(row.normalized?.clientRowId()).isEqualTo("3")
		}

		@Test
		@DisplayName("도수와 용량의 단위 표기를 공통 요청에 그대로 전달한다")
		fun validate_whenAbvAndVolumeHaveUnits_delegatesToBulkService() {
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
							tagOak.id.toString()
						)
					)
				}

			val result = service.validate(file)
			assertThat(result.rows[0].valid).isTrue()
			assertThat(bulkService.receivedRequests.single().rows().single().abv()).isEqualTo("40%")
			assertThat(bulkService.receivedRequests.single().rows().single().volume()).isEqualTo("700ml")
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
							tagOak.id.toString()
						)
					)
				}

			val result = service.validate(file)
			assertThat(result.rows[0].errors.map { it.code }).contains("REGION_NOT_FOUND")
		}

		@Test
		@DisplayName("파일 내부 중복과 기존 후보는 공통 검증의 warning 을 보존한다")
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
					.build()
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
					tagOak.id.toString()
				)
			val file =
				workbookAsMultipart { workbook ->
					writeDataRow(workbook, values, rowIndex = 2)
					writeDataRow(workbook, values, rowIndex = 3)
				}

			val result = service.validate(file)
			assertThat(result.rows).allMatch { row -> row.warnings.any { it.code == "DUPLICATE_IN_FILE" } }
			assertThat(result.rows).allMatch { row -> row.warnings.any { it.code == "DUPLICATE_CANDIDATE" } }
			assertThat(result.rows[0].warnings[0].message).contains("이미 등록된 위스키입니다")
		}

		@Test
		@DisplayName("이름·증류소·도수가 같아도 용량이 다르면 DB 중복 후보가 아니다")
		fun validate_whenVolumeDiffers_isNotDbDuplicateCandidate() {
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
					.build()
			)

			val file =
				workbookAsMultipart { workbook ->
					writeDataRow(
						workbook,
						validRowValues().toMutableList().apply {
							set(AlcoholExcelSchema.Column.VOLUME.index, "750.00")
						}
					)
				}

			val result = service.validate(file)

			assertThat(result.rows.single().warnings.map { it.code }).doesNotContain("DUPLICATE_CANDIDATE")
		}

		@Test
		@DisplayName("40과 40.00은 파일 내부 중복 warning 으로 본다")
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
							tagOak.id.toString()
						),
						rowIndex = 2
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
							tagOak.id.toString()
						),
						rowIndex = 3
					)
				}

			val result = service.validate(file)
			assertThat(result.rows).allMatch { row -> row.warnings.any { it.code == "DUPLICATE_IN_FILE" } }
		}

		@Test
		@DisplayName("01과 1 증류소 ID는 파일 내부 중복 warning 에서 같은 ID로 본다")
		fun validate_whenDistilleryIdHasLeadingZero_isDuplicateInFile() {
			val file =
				workbookAsMultipart { workbook ->
					writeDataRow(workbook, validRowValues(), rowIndex = 2)
					writeDataRow(
						workbook,
						validRowValues().toMutableList().apply {
							set(AlcoholExcelSchema.Column.DISTILLERY_ID.index, "0${distillery.id}")
						},
						rowIndex = 3
					)
				}

			val result = service.validate(file)

			assertThat(result.rows).allMatch { row -> row.warnings.any { it.code == "DUPLICATE_IN_FILE" } }
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
		@DisplayName("손상된 xlsx ZIP은 파일 형식 오류로 반환한다")
		fun validate_whenXlsxZipIsCorrupt_returnsInvalidFileType() {
			val file =
				MockMultipartFile("file", "corrupt.xlsx", AlcoholExcelSchema.XLSX_CONTENT_TYPE, "not-a-zip".toByteArray())

			assertThatThrownBy { service.validate(file) }
				.isInstanceOf(AlcoholException::class.java)
				.extracting("exceptionCode")
				.isEqualTo(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE)
		}

		@Test
		@DisplayName("5MB를 초과한 파일은 EXCEL_FILE_TOO_LARGE 예외를 반환한다")
		fun validate_whenFileExceedsLimit_rejectsFile() {
			val file =
				MockMultipartFile(
					"file",
					"alcohol-import.xlsx",
					AlcoholExcelSchema.XLSX_CONTENT_TYPE,
					ByteArray((AlcoholExcelSchema.MAX_FILE_BYTES + 1).toInt())
				)

			assertThatThrownBy { service.validate(file) }
				.isInstanceOf(AlcoholException::class.java)
				.extracting("exceptionCode")
				.isEqualTo(AlcoholExceptionCode.EXCEL_FILE_TOO_LARGE)
		}

		@Test
		@DisplayName("ZIP entry가 200개를 초과하면 EXCEL_INVALID_FILE_TYPE 예외를 반환한다")
		fun validate_whenZipHasTooManyEntries_rejectsFile() {
			val output = ByteArrayOutputStream()
			ZipOutputStream(output).use { zip ->
				repeat(201) { index ->
					zip.putNextEntry(ZipEntry("entry-$index.xml"))
					zip.write(1)
					zip.closeEntry()
				}
			}
			val file =
				MockMultipartFile(
					"file",
					"many-parts.xlsx",
					AlcoholExcelSchema.XLSX_CONTENT_TYPE,
					output.toByteArray()
				)

			assertThatThrownBy { service.validate(file) }
				.isInstanceOf(AlcoholException::class.java)
				.extracting("exceptionCode")
				.isEqualTo(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE)
		}

		@Test
		@DisplayName("고정 헤더가 변경되면 EXCEL_HEADER_MISMATCH 예외를 반환한다")
		fun validate_whenHeaderChanges_rejectsWorkbook() {
			val file =
				workbookAsMultipart { workbook ->
					workbook
						.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
						.getRow(AlcoholExcelSchema.HEADER_ROW_INDEX)
						.getCell(AlcoholExcelSchema.Column.KOR_NAME.index)
						.setCellValue("변경된 헤더")
				}

			assertThatThrownBy { service.validate(file) }
				.isInstanceOf(AlcoholException::class.java)
				.extracting("exceptionCode")
				.isEqualTo(AlcoholExceptionCode.EXCEL_HEADER_MISMATCH)
		}

		@Test
		@DisplayName("데이터가 1,000행을 초과하면 EXCEL_ROW_LIMIT_EXCEEDED 예외를 반환한다")
		fun validate_whenRowsExceedLimit_rejectsWorkbook() {
			val file =
				workbookAsMultipart { workbook ->
					repeat(AlcoholExcelSchema.MAX_DATA_ROWS + 1) { index ->
						writeDataRow(
							workbook,
							validRowValues(),
							AlcoholExcelSchema.DATA_START_ROW_INDEX + index
						)
					}
				}

			assertThatThrownBy { service.validate(file) }
				.isInstanceOf(AlcoholException::class.java)
				.extracting("exceptionCode")
				.isEqualTo(AlcoholExceptionCode.EXCEL_ROW_LIMIT_EXCEEDED)
		}

		@Test
		@DisplayName("Long 범위를 넘는 참조 ID는 INVALID_ID 오류로 반환한다")
		fun validate_whenReferenceIdOverflows_returnsInvalidId() {
			val file =
				workbookAsMultipart { workbook ->
					writeDataRow(
						workbook,
						validRowValues().toMutableList().apply {
							set(AlcoholExcelSchema.Column.REGION_ID.index, "999999999999999999999999")
						}
					)
				}

			val result = service.validate(file)

			assertThat(result.rows.single().errors.map { it.code }).contains("INVALID_ID")
			assertThat(result.rows.single().valid).isFalse()
			assertThat(result.rows.single().normalized).isNull()
		}

		@Test
		@DisplayName("카테고리 안정 키의 그룹과 별도 그룹이 다르면 입력한 그룹을 사용하고 warning 을 반환한다")
		fun validate_whenCategoryGroupDiffers_usesExplicitGroupWithWarning() {
			val file =
				workbookAsMultipart { workbook ->
					writeDataRow(
						workbook,
						validRowValues().toMutableList().apply {
							set(AlcoholExcelSchema.Column.CATEGORY_GROUP.index, "BLEND")
						}
					)
				}

			val result = service.validate(file)

			assertThat(result.rows.single().warnings.map { it.code }).contains("CATEGORY_GROUP_MISMATCH")
			assertThat(bulkService.receivedRequests.single().rows().single().categoryGroup()).isEqualTo("BLEND")
		}

		@Test
		@DisplayName("빈 템플릿은 공통 서비스 호출 없이 0행 결과를 반환한다")
		fun validate_whenTemplateIsEmpty_returnsZeroRows() {
			val result = service.validate(workbookAsMultipart { })

			assertThat(result.totalRows).isZero()
			assertThat(bulkService.receivedRequests).isEmpty()
		}

		@Test
		@DisplayName("시트 순서와 참조 시트 변경은 허용하지만 데이터 열 추가는 거절한다")
		fun validate_whenSheetOrderChanges_acceptsButExtraDataColumnRejects() {
			val reordered =
				workbookAsMultipart { workbook ->
					workbook.setSheetOrder(AlcoholExcelSchema.DATA_SHEET_NAME, 0)
					workbook.removeSheetAt(workbook.getSheetIndex(AlcoholExcelSchema.REGION_SHEET_NAME))
					workbook.createSheet("메모")
					writeDataRow(workbook, validRowValues())
				}
			assertThat(service.validate(reordered).validRows).isEqualTo(1)

			val withExtraColumn =
				workbookAsMultipart { workbook ->
					writeDataRow(workbook, validRowValues())
					workbook
						.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
						.getRow(2)
						.createCell(13)
						.setCellValue("지원하지 않음")
				}
			assertThatThrownBy { service.validate(withExtraColumn) }
				.isInstanceOf(AlcoholException::class.java)
				.extracting("exceptionCode")
				.isEqualTo(AlcoholExceptionCode.EXCEL_HEADER_MISMATCH)
		}

		@Test
		@DisplayName("퍼센트 서식 숫자 셀은 퍼센트 값으로, 일반 숫자 셀은 원래 값으로 공통 요청에 전달한다")
		fun validate_whenNumericPercentCell_convertsOnlyPercentageFormat() {
			val file =
				workbookAsMultipart { workbook ->
					writeDataRow(workbook, validRowValues())
					val row = workbook.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME).getRow(2)
					row.getCell(AlcoholExcelSchema.Column.ABV.index).setCellValue(0.4)
					val percentageStyle =
						workbook.createCellStyle().apply {
							dataFormat = workbook.createDataFormat().getFormat("0%")
						}
					row.getCell(AlcoholExcelSchema.Column.ABV.index).cellStyle = percentageStyle
				}

			service.validate(file)

			assertThat(bulkService.receivedRequests.single().rows().single().abv()).isEqualTo("40%")

			val literalPercent =
				workbookAsMultipart { workbook ->
					writeDataRow(workbook, validRowValues())
					val row = workbook.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME).getRow(2)
					row.getCell(AlcoholExcelSchema.Column.ABV.index).setCellValue(40.0)
					val literalPercentStyle =
						workbook.createCellStyle().apply {
							dataFormat = workbook.createDataFormat().getFormat("0\\%")
						}
					row.getCell(AlcoholExcelSchema.Column.ABV.index).cellStyle = literalPercentStyle
				}
			val literalResult = service.validate(literalPercent)

			assertThat(bulkService.receivedRequests[1].rows().single().abv()).isEqualTo("40")
			assertThat(literalResult.rows.single().abv).isEqualTo("40%")

			val generalNumber =
				workbookAsMultipart { workbook ->
					writeDataRow(workbook, validRowValues())
					workbook
						.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
						.getRow(2)
						.getCell(AlcoholExcelSchema.Column.ABV.index)
						.setCellValue(0.4)
				}
			service.validate(generalNumber)

			assertThat(bulkService.receivedRequests[2].rows().single().abv()).isEqualTo("0.4")
		}

		@Test
		@DisplayName("수식 셀이 있으면 EXCEL_FORMULA_NOT_ALLOWED 예외를 반환한다")
		fun validate_whenFormulaExists_rejectsWorkbook() {
			val file =
				workbookAsMultipart { workbook ->
					writeDataRow(workbook, validRowValues())
					workbook
						.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
						.getRow(AlcoholExcelSchema.DATA_START_ROW_INDEX)
						.getCell(AlcoholExcelSchema.Column.ABV.index)
						.setCellFormula("20+20")
				}

			assertThatThrownBy { service.validate(file) }
				.isInstanceOf(AlcoholException::class.java)
				.extracting("exceptionCode")
				.isEqualTo(AlcoholExceptionCode.EXCEL_FORMULA_NOT_ALLOWED)
		}

		@Test
		@DisplayName("참조 시트에 수식 셀이 있어도 EXCEL_FORMULA_NOT_ALLOWED 예외를 반환한다")
		fun validate_whenReferenceSheetContainsFormula_rejectsWorkbook() {
			val file =
				workbookAsMultipart { workbook ->
					workbook
						.getSheet(AlcoholExcelSchema.REGION_SHEET_NAME)
						.getRow(1)
						.getCell(0)
						.setCellFormula("1+1")
				}

			assertThatThrownBy { service.validate(file) }
				.isInstanceOf(AlcoholException::class.java)
				.extracting("exceptionCode")
				.isEqualTo(AlcoholExceptionCode.EXCEL_FORMULA_NOT_ALLOWED)
		}

		@Test
		@DisplayName("외부 하이퍼링크가 있으면 EXCEL_EXTERNAL_LINK_NOT_ALLOWED 예외를 반환한다")
		fun validate_whenExternalHyperlinkExists_rejectsWorkbook() {
			val file =
				workbookAsMultipart { workbook ->
					val hyperlink = workbook.creationHelper.createHyperlink(HyperlinkType.URL)
					hyperlink.address = "https://example.com/external"
					workbook
						.getSheet(AlcoholExcelSchema.GUIDE_SHEET_NAME)
						.getRow(0)
						.getCell(0)
						.hyperlink = hyperlink
				}

			assertThatThrownBy { service.validate(file) }
				.isInstanceOf(AlcoholException::class.java)
				.extracting("exceptionCode")
				.isEqualTo(AlcoholExceptionCode.EXCEL_EXTERNAL_LINK_NOT_ALLOWED)
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
			output.toByteArray()
		)
	}

	private fun writeDataRow(
		workbook: XSSFWorkbook,
		values: List<String>,
		rowIndex: Int = 2
	) {
		val sheet = workbook.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
		val row = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
		values.forEachIndexed { index, value ->
			val cell = row.getCell(index) ?: row.createCell(index)
			cell.setCellValue(value)
		}
	}

	private fun validRowValues(): List<String> = listOf(
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
		"설명",
		"700.00",
		tagOak.id.toString()
	)

	private class FakeAdminAlcoholBulkService : AdminAlcoholBulkService {
		val receivedRequests = mutableListOf<AdminAlcoholBulkRequest>()

		override fun validate(request: AdminAlcoholBulkRequest): AdminAlcoholBulkValidateResponse {
			receivedRequests += request
			val duplicateCounts = request.rows().groupingBy(::identity).eachCount()
			val rows = request.rows().map { row ->
				val errors = mutableListOf<AdminAlcoholBulkIssue>()
				val warnings = mutableListOf<AdminAlcoholBulkIssue>()
				if (row.regionId() == 999999L) {
					errors += AdminAlcoholBulkIssue("REGION_NOT_FOUND", "regionId", "지역 ID를 찾을 수 없습니다.")
				}
				if ((duplicateCounts[identity(row)] ?: 0) > 1) {
					warnings += AdminAlcoholBulkIssue("DUPLICATE_IN_FILE", null, "파일 내부 중복 후보입니다.")
				}
				if (row.categoryGroup() == "BLEND") {
					warnings += AdminAlcoholBulkIssue("CATEGORY_GROUP_MISMATCH", "categoryGroup", "기존 카테고리와 그룹이 다릅니다.")
				}
				val candidateIds =
					if (row.korName() == "글렌피딕 12년" && row.volume().startsWith("700")) listOf(777L) else emptyList()
				if (candidateIds.isNotEmpty()) {
					warnings += AdminAlcoholBulkIssue("DUPLICATE_CANDIDATE", null, "기존 등록 후보입니다.")
				}
				val normalized = if (errors.isEmpty()) normalize(row) else null
				AdminAlcoholBulkRowResult(row.clientRowId(), errors.isEmpty(), normalized, errors, warnings, candidateIds)
			}
			return AdminAlcoholBulkValidateResponse(
				rows.size,
				rows.count { it.valid() },
				rows.count { !it.valid() },
				rows.count { it.warnings().isNotEmpty() },
				rows
			)
		}

		override fun create(request: AdminAlcoholBulkRequest): AdminAlcoholBulkCreateResponse {
			val validation = validate(request)
			return AdminAlcoholBulkCreateResponse(0, emptyList(), validation)
		}

		private fun normalize(row: AdminAlcoholBulkRowRequest): AdminAlcoholBulkRowRequest = AdminAlcoholBulkRowRequest(
			row.clientRowId(),
			row.korName(),
			row.engName(),
			normalizeNumber(row.abv(), "%"),
			if (row.type() == "위스키") "WHISKY" else row.type(),
			row.korCategory(),
			row.engCategory(),
			row.categoryGroup(),
			row.regionId(),
			row.distilleryId(),
			row.age(),
			row.cask(),
			row.description(),
			normalizeNumber(row.volume(), "ml"),
			row.tastingTagIds(),
			row.imageUrl()
		)

		private fun normalizeNumber(value: String, suffix: String): String {
			val number = value.removeSuffix("%").removeSuffix("ml").trim()
			return BigDecimal(number).stripTrailingZeros().toPlainString() + suffix
		}

		private fun identity(row: AdminAlcoholBulkRowRequest): String = listOf(
			row.korName(),
			row.distilleryId(),
			normalizeNumber(row.abv(), "%"),
			normalizeNumber(row.volume(), "ml")
		).joinToString("|")
	}
}
