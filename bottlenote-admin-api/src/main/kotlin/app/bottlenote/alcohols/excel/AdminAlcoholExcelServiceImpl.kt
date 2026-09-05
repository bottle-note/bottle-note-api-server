package app.bottlenote.alcohols.excel

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup
import app.bottlenote.alcohols.constant.AlcoholType
import app.bottlenote.alcohols.domain.AlcoholQueryRepository
import app.bottlenote.alcohols.domain.DistilleryRepository
import app.bottlenote.alcohols.domain.RegionRepository
import app.bottlenote.alcohols.domain.TastingTagRepository
import app.bottlenote.alcohols.dto.request.AdminAlcoholBulkRequest
import app.bottlenote.alcohols.dto.request.AdminAlcoholBulkRowRequest
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkIssueItem
import app.bottlenote.alcohols.dto.response.AdminAlcoholBulkRowItem
import app.bottlenote.alcohols.dto.response.AdminAlcoholExcelIssue
import app.bottlenote.alcohols.dto.response.AdminAlcoholExcelRowResult
import app.bottlenote.alcohols.dto.response.AdminAlcoholExcelValidateResponse
import app.bottlenote.alcohols.dto.response.AdminDistilleryItem
import app.bottlenote.alcohols.dto.response.AdminRegionItem
import app.bottlenote.alcohols.dto.response.AlcoholBulkCategoryItem
import app.bottlenote.alcohols.dto.response.CategoryItem
import app.bottlenote.alcohols.dto.response.TastingTagNodeItem
import app.bottlenote.alcohols.excel.AlcoholExcelSchema.Column
import app.bottlenote.alcohols.exception.AlcoholException
import app.bottlenote.alcohols.exception.AlcoholExceptionCode
import app.bottlenote.alcohols.service.AdminAlcoholBulkService
import org.apache.poi.ooxml.POIXMLException
import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.openxml4j.util.ZipSecureFile
import org.apache.poi.ss.format.CellFormatPart
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataValidation
import org.apache.poi.ss.usermodel.DataValidationHelper
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Name
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigDecimal
import java.util.Locale
import java.util.zip.ZipInputStream

@Service
class AdminAlcoholExcelServiceImpl(
	private val regionRepository: RegionRepository,
	private val distilleryRepository: DistilleryRepository,
	private val tastingTagRepository: TastingTagRepository,
	private val alcoholQueryRepository: AlcoholQueryRepository,
	private val adminAlcoholBulkService: AdminAlcoholBulkService
) : AdminAlcoholExcelService {
	companion object {
		private const val REFERENCE_PAGE_SIZE = 1_000
		private const val DESCRIPTION_MARKER = "AlcoholImportDescriptionRow"
		private const val DESCRIPTION_REFERENCE = "'알코올 데이터'!\$A\$2:\$M\$2"
		private const val MAX_ZIP_ENTRIES = 200
		private const val MAX_TOTAL_UNCOMPRESSED_BYTES = 50L * 1024 * 1024

		init {
			ZipSecureFile.setMinInflateRatio(0.01)
			ZipSecureFile.setMaxEntrySize(AlcoholExcelSchema.MAX_FILE_BYTES)
		}
	}

	override fun createTemplateWorkbook(): ByteArray {
		XSSFWorkbook().use { workbook ->
			val styles = TemplateStyles(workbook)

			val guideSheet = workbook.createSheet(AlcoholExcelSchema.GUIDE_SHEET_NAME)
			val regionSheet = workbook.createSheet(AlcoholExcelSchema.REGION_SHEET_NAME)
			val distillerySheet = workbook.createSheet(AlcoholExcelSchema.DISTILLERY_SHEET_NAME)
			val tagSheet = workbook.createSheet(AlcoholExcelSchema.TASTING_TAG_SHEET_NAME)
			val categorySheet = workbook.createSheet(AlcoholExcelSchema.CATEGORY_SHEET_NAME)
			val dataSheet = workbook.createSheet(AlcoholExcelSchema.DATA_SHEET_NAME)

			val regions = loadRegions()
			val distilleries = loadDistilleries()
			val tags = loadTastingTags()
			val categories = loadBulkReferenceCategories()

			writeGuideSheet(guideSheet, styles)
			writeReferenceSheet(
				regionSheet,
				listOf("ID", "한글 이름", "영문 이름"),
				regions.map { listOf(it.id.toString(), it.korName.orEmpty(), it.engName.orEmpty()) },
				styles
			)
			writeReferenceSheet(
				distillerySheet,
				listOf("ID", "한글 이름", "영문 이름"),
				distilleries.map { listOf(it.id.toString(), it.korName.orEmpty(), it.engName.orEmpty()) },
				styles
			)
			writeReferenceSheet(
				tagSheet,
				listOf("ID", "한글 이름", "영문 이름"),
				tags.map { listOf(it.id.toString(), it.korName.orEmpty(), it.engName.orEmpty()) },
				styles
			)
			// 카테고리는 안정 키(그룹|한글|영문)를 ID 칸에 노출한다.
			writeReferenceSheet(
				categorySheet,
				listOf("ID", "카테고리 그룹", "한글 카테고리", "영문 카테고리"),
				categories.map { item ->
					listOf(
						categoryStableId(item),
						item.categoryGroup()?.description.orEmpty(),
						item.korCategory().orEmpty(),
						item.engCategory().orEmpty()
					)
				},
				styles
			)
			writeHeaderAndDescription(dataSheet, styles)
			workbook.createName().apply {
				nameName = DESCRIPTION_MARKER
				refersToFormula = DESCRIPTION_REFERENCE
			}
			addDropdownValidations(
				workbook = workbook,
				dataSheet = dataSheet,
				regionCount = regions.size,
				distilleryCount = distilleries.size,
				tagCount = tags.size,
				categoryCount = categories.size
			)

			AlcoholExcelSchema.HEADERS.indices.forEach { dataSheet.autoSizeColumn(it) }
			workbook.setActiveSheet(workbook.getSheetIndex(AlcoholExcelSchema.GUIDE_SHEET_NAME))

			return ByteArrayOutputStream().use { out ->
				workbook.write(out)
				out.toByteArray()
			}
		}
	}

	override fun validate(file: MultipartFile): AdminAlcoholExcelValidateResponse {
		validateFileEnvelope(file)

		val bytes = file.bytes
		openSecureWorkbook(bytes).use { workbook ->
			validateWorkbookStructure(workbook)
			val dataSheet =
				workbook.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
					?: throw AlcoholException(AlcoholExceptionCode.EXCEL_SHEET_NOT_FOUND)

			rejectUnsafeWorkbook(workbook)

			val parsedRows = mutableListOf<ParsedRow>()
			for (row in dataSheet) {
				if (row.rowNum < AlcoholExcelSchema.DATA_START_ROW_INDEX) continue
				if (isCompletelyBlank(row)) continue
				parsedRows += parseRow(row.rowNum, row)
				if (parsedRows.size > AlcoholExcelSchema.MAX_DATA_ROWS) {
					throw AlcoholException(AlcoholExceptionCode.EXCEL_ROW_LIMIT_EXCEEDED)
				}
			}

			if (parsedRows.isEmpty()) {
				return AdminAlcoholExcelValidateResponse(0, 0, 0, 0, emptyList())
			}

			val adapters = parsedRows.map(::toBulkAdapterRow)
			val commonRows =
				adminAlcoholBulkService
					.validate(AdminAlcoholBulkRequest(adapters.map { it.request }))
					.rows()
					.associateBy { it.clientRowId() }
			val regionsById = loadRegions().associateBy { it.id }
			val distilleriesById = loadDistilleries().associateBy { it.id }
			val results = adapters.map { adapter ->
				adaptResult(adapter, commonRows[adapter.request.clientRowId()], regionsById, distilleriesById)
			}

			return AdminAlcoholExcelValidateResponse(
				totalRows = results.size,
				validRows = results.count { it.valid },
				invalidRows = results.count { !it.valid },
				warningRows = results.count { it.warnings.isNotEmpty() },
				rows = results
			)
		}
	}

	private fun validateFileEnvelope(file: MultipartFile) {
		if (file.isEmpty) throw AlcoholException(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE)
		if (file.size > AlcoholExcelSchema.MAX_FILE_BYTES) throw AlcoholException(AlcoholExceptionCode.EXCEL_FILE_TOO_LARGE)
		val original = file.originalFilename?.lowercase(Locale.ROOT).orEmpty()
		val contentType = file.contentType.orEmpty()
		val looksLikeXlsx =
			original.endsWith(".xlsx") ||
				contentType.equals(AlcoholExcelSchema.XLSX_CONTENT_TYPE, ignoreCase = true) ||
				contentType.equals("application/octet-stream", ignoreCase = true)
		if (!looksLikeXlsx || original.endsWith(".csv") || original.endsWith(".xls") || original.endsWith(".zip")) {
			throw AlcoholException(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE)
		}
	}

	private fun openSecureWorkbook(bytes: ByteArray): Workbook {
		var pkg: OPCPackage? = null
		return try {
			preflightZip(bytes)
			pkg = OPCPackage.open(ByteArrayInputStream(bytes))
			XSSFWorkbook(pkg)
		} catch (exception: AlcoholException) {
			throw exception
		} catch (_: IOException) {
			pkg?.close()
			throw AlcoholException(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE)
		} catch (_: InvalidFormatException) {
			pkg?.close()
			throw AlcoholException(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE)
		} catch (_: POIXMLException) {
			pkg?.close()
			throw AlcoholException(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE)
		} catch (_: IllegalArgumentException) {
			pkg?.close()
			throw AlcoholException(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE)
		}
	}

	private fun preflightZip(bytes: ByteArray) {
		ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
			val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
			var entryCount = 0
			var totalUncompressedBytes = 0L
			while (zip.nextEntry != null) {
				entryCount++
				if (entryCount > MAX_ZIP_ENTRIES) {
					throw AlcoholException(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE)
				}
				while (true) {
					val read = zip.read(buffer)
					if (read < 0) break
					totalUncompressedBytes += read
					if (totalUncompressedBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
						throw AlcoholException(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE)
					}
				}
				zip.closeEntry()
			}
			if (entryCount == 0) {
				throw AlcoholException(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE)
			}
		}
	}

	private fun validateWorkbookStructure(workbook: Workbook) {
		val dataSheet = workbook.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
			?: throw AlcoholException(AlcoholExceptionCode.EXCEL_SHEET_NOT_FOUND)
		val headerRow =
			dataSheet.getRow(AlcoholExcelSchema.HEADER_ROW_INDEX)
				?: throw AlcoholException(AlcoholExceptionCode.EXCEL_HEADER_MISMATCH)
		val descriptionRow = dataSheet.getRow(AlcoholExcelSchema.DESCRIPTION_ROW_INDEX)
			?: throw AlcoholException(AlcoholExceptionCode.EXCEL_DESCRIPTION_MISMATCH)
		val marker = workbook.getName(DESCRIPTION_MARKER)
		val recognizableLegacyDescription = AlcoholExcelSchema.DESCRIPTIONS.indices.any {
			readRawCell(descriptionRow.getCell(it)) == AlcoholExcelSchema.DESCRIPTIONS[it]
		} ||
			isCompletelyBlank(descriptionRow)
		if ((marker != null && marker.refersToFormula != DESCRIPTION_REFERENCE) ||
			(marker == null && !recognizableLegacyDescription)
		) {
			throw AlcoholException(AlcoholExceptionCode.EXCEL_DESCRIPTION_MISMATCH)
		}

		val headers = AlcoholExcelSchema.HEADERS.indices.map { readRawCell(headerRow.getCell(it)) }
		if (headers != AlcoholExcelSchema.HEADERS) {
			if (headers.size != headers.distinct().size) throw AlcoholException(AlcoholExceptionCode.EXCEL_DUPLICATE_HEADER)
			throw AlcoholException(AlcoholExceptionCode.EXCEL_HEADER_MISMATCH)
		}
		if (headerRow.any { it.columnIndex >= AlcoholExcelSchema.HEADERS.size && readRawCell(it).isNotBlank() }) {
			throw AlcoholException(AlcoholExceptionCode.EXCEL_HEADER_MISMATCH)
		}
		for (row in dataSheet) {
			if (row.rowNum < AlcoholExcelSchema.DATA_START_ROW_INDEX) continue
			if (row.any { it.columnIndex >= AlcoholExcelSchema.HEADERS.size && readRawCell(it).isNotBlank() }) {
				throw AlcoholException(AlcoholExceptionCode.EXCEL_HEADER_MISMATCH)
			}
		}
	}

	private fun rejectUnsafeWorkbook(workbook: Workbook) {
		if (workbook is XSSFWorkbook && workbook.externalLinksTable.isNotEmpty()) {
			throw AlcoholException(AlcoholExceptionCode.EXCEL_EXTERNAL_LINK_NOT_ALLOWED)
		}
		for (sheetIndex in 0 until workbook.numberOfSheets) {
			val sheet = workbook.getSheetAt(sheetIndex)
			for (row in sheet) {
				for (cell in row) {
					if (cell.cellType == CellType.FORMULA || cell.cellType == CellType.ERROR) {
						throw AlcoholException(AlcoholExceptionCode.EXCEL_FORMULA_NOT_ALLOWED)
					}
					val hyperlinkAddress = cell.hyperlink?.address
					if (!hyperlinkAddress.isNullOrBlank() && !hyperlinkAddress.startsWith("#")) {
						throw AlcoholException(AlcoholExceptionCode.EXCEL_EXTERNAL_LINK_NOT_ALLOWED)
					}
				}
			}
		}
	}

	private fun parseRow(
		rowIndex: Int,
		row: Row
	): ParsedRow {
		fun cell(column: Column): String = readRawCell(row.getCell(column.index), percentageFormatted = column == Column.ABV)
		return ParsedRow(
			rowNumber = rowIndex + 1,
			korName = cell(Column.KOR_NAME),
			engName = cell(Column.ENG_NAME),
			abv = cell(Column.ABV),
			type = cell(Column.TYPE),
			categoryId = cell(Column.CATEGORY_ID),
			categoryGroup = cell(Column.CATEGORY_GROUP),
			regionId = cell(Column.REGION_ID),
			distilleryId = cell(Column.DISTILLERY_ID),
			age = cell(Column.AGE),
			cask = cell(Column.CASK),
			description = cell(Column.DESCRIPTION),
			volume = cell(Column.VOLUME),
			tastingTagIds = cell(Column.TASTING_TAG_IDS)
		)
	}

	private fun toBulkAdapterRow(parsed: ParsedRow): BulkAdapterRow {
		val errors = mutableListOf<AdminAlcoholExcelIssue>()
		val categoryParts = parsed.categoryId.trim().split("|").map(String::trim)
		val validCategory = categoryParts.size == 3 && categoryParts.all(String::isNotBlank)
		if (!validCategory) {
			errors += issue("INVALID_ID", Column.CATEGORY_ID.header, "카테고리 ID는 그룹|한글|영문 형식으로 입력해야 합니다.")
		}
		val categoryGroup = parsed.categoryGroup.ifBlank { categoryParts.getOrNull(0).orEmpty() }
		val regionId = parseInputId(parsed.regionId, Column.REGION_ID, errors)
		val distilleryId = parseInputId(parsed.distilleryId, Column.DISTILLERY_ID, errors)
		val tastingTagIds = parseTastingTagIds(parsed.tastingTagIds, errors)
		return BulkAdapterRow(
			parsed = parsed,
			request = AdminAlcoholBulkRowRequest(
				parsed.rowNumber.toString(),
				parsed.korName,
				parsed.engName,
				parsed.abv,
				parsed.type,
				categoryParts.getOrNull(1),
				categoryParts.getOrNull(2),
				categoryGroup,
				regionId,
				distilleryId,
				parsed.age.ifBlank { null },
				parsed.cask.ifBlank { null },
				parsed.description.ifBlank { null },
				parsed.volume,
				tastingTagIds,
				null
			),
			errors = errors,
			warnings = emptyList()
		)
	}

	private fun adaptResult(
		adapter: BulkAdapterRow,
		common: AdminAlcoholBulkRowItem?,
		regionsById: Map<Long, AdminRegionItem>,
		distilleriesById: Map<Long, AdminDistilleryItem>
	): AdminAlcoholExcelRowResult {
		val commonErrors =
			common?.errors().orEmpty().map(::adaptIssue).ifEmpty {
				if (common == null) {
					listOf(issue("BULK_ROW_RESULT_MISSING", null, "공통 검증 결과에서 행을 찾을 수 없습니다."))
				} else {
					emptyList()
				}
			}
		val commonWarnings = common?.warnings().orEmpty().map(::adaptIssue)
		val errors = adapter.errors + commonErrors
		val normalized = common?.normalized()?.takeIf { errors.isEmpty() && common.valid() }
		return AdminAlcoholExcelRowResult(
			rowNumber = adapter.parsed.rowNumber,
			clientRowId = adapter.request.clientRowId(),
			korName = normalized?.korName() ?: adapter.parsed.korName.ifBlank { null },
			engName = normalized?.engName() ?: adapter.parsed.engName.ifBlank { null },
			abv = normalized?.abv() ?: adapter.parsed.abv.ifBlank { null },
			type = normalized?.type() ?: adapter.parsed.type.ifBlank { null },
			korCategory = normalized?.korCategory() ?: adapter.request.korCategory(),
			engCategory = normalized?.engCategory() ?: adapter.request.engCategory(),
			categoryGroup = normalized?.categoryGroup() ?: adapter.request.categoryGroup(),
			region = normalized?.regionId()?.let { regionsById[it]?.korName() },
			distillery = normalized?.distilleryId()?.let { distilleriesById[it]?.korName() },
			age = normalized?.age() ?: adapter.request.age(),
			cask = normalized?.cask() ?: adapter.request.cask(),
			description = normalized?.description() ?: adapter.request.description(),
			volume = normalized?.volume() ?: adapter.parsed.volume.ifBlank { null },
			tastingTags = adapter.parsed.tastingTagIds.ifBlank { null },
			regionId = normalized?.regionId() ?: adapter.request.regionId(),
			distilleryId = normalized?.distilleryId() ?: adapter.request.distilleryId(),
			tastingTagIds = normalized?.tastingTagIds() ?: adapter.request.tastingTagIds(),
			candidateAlcoholIds = common?.candidateAlcoholIds()?.takeIf { it.isNotEmpty() },
			valid = errors.isEmpty() && common?.valid() == true,
			errors = errors,
			warnings = adapter.warnings + commonWarnings,
			normalized = normalized
		)
	}

	private fun parseInputId(raw: String, column: Column, errors: MutableList<AdminAlcoholExcelIssue>): Long? {
		if (raw.isBlank()) return null
		if (!raw.trim().matches(Regex("""^\d+$"""))) {
			errors += issue("INVALID_ID", column.header, "${column.header} 필드는 Long 범위의 숫자여야 합니다.")
			return null
		}
		return raw.trim().toLongOrNull() ?: run {
			errors += issue("INVALID_ID", column.header, "${column.header} 필드는 Long 범위의 숫자여야 합니다.")
			null
		}
	}

	private fun parseTastingTagIds(raw: String, errors: MutableList<AdminAlcoholExcelIssue>): List<Long>? {
		if (raw.isBlank()) return emptyList()
		return raw.split("|").mapNotNull { value ->
			parseInputId(value.trim(), Column.TASTING_TAG_IDS, errors)
		}
	}

	private fun adaptIssue(issue: AdminAlcoholBulkIssueItem): AdminAlcoholExcelIssue = AdminAlcoholExcelIssue(excelIssueCode(issue), excelFieldName(issue.field()), issue.message())

	private fun excelIssueCode(issue: AdminAlcoholBulkIssueItem): String = when (issue.code()) {
		"REQUIRED" -> "REQUIRED_FIELD"
		"INVALID_QUANTITY" -> "INVALID_NUMBER"
		"INVALID_ENUM" -> "INVALID_ENUM_VALUE"
		"INVALID_REFERENCE" ->
			when (issue.field()) {
				"regionId" -> "REGION_NOT_FOUND"
				"distilleryId" -> "DISTILLERY_NOT_FOUND"
				"tastingTagIds" -> "TASTING_TAG_NOT_FOUND"
				else -> "INVALID_ID"
			}
		"DUPLICATE_TAG_REMOVED" -> "DUPLICATE_TASTING_TAG"
		"DUPLICATE_REQUEST_ROW" -> "DUPLICATE_IN_FILE"
		"DUPLICATE_DB_CANDIDATE" -> "DUPLICATE_CANDIDATE"
		else -> issue.code()
	}

	private fun excelFieldName(field: String?): String? = mapOf(
		"korName" to Column.KOR_NAME.header,
		"engName" to Column.ENG_NAME.header,
		"abv" to Column.ABV.header,
		"type" to Column.TYPE.header,
		"korCategory" to Column.CATEGORY_ID.header,
		"engCategory" to Column.CATEGORY_ID.header,
		"categoryGroup" to Column.CATEGORY_GROUP.header,
		"regionId" to Column.REGION_ID.header,
		"distilleryId" to Column.DISTILLERY_ID.header,
		"age" to Column.AGE.header,
		"cask" to Column.CASK.header,
		"description" to Column.DESCRIPTION.header,
		"volume" to Column.VOLUME.header,
		"tastingTagIds" to Column.TASTING_TAG_IDS.header
	)[field] ?: field

	private fun writeHeaderAndDescription(
		sheet: Sheet,
		styles: TemplateStyles
	) {
		val headerRow = sheet.createRow(AlcoholExcelSchema.HEADER_ROW_INDEX)
		val descriptionRow = sheet.createRow(AlcoholExcelSchema.DESCRIPTION_ROW_INDEX)
		AlcoholExcelSchema.HEADERS.forEachIndexed { index, header ->
			headerRow.createCell(index).apply {
				setCellValue(header)
				cellStyle = styles.header
			}
			descriptionRow.createCell(index).apply {
				setCellValue(AlcoholExcelSchema.DESCRIPTIONS[index])
				cellStyle = styles.description
			}
		}
		headerRow.heightInPoints = 22f
		descriptionRow.heightInPoints = 40f
		sheet.createFreezePane(0, AlcoholExcelSchema.DATA_START_ROW_INDEX)
	}

	private fun writeReferenceSheet(
		sheet: Sheet,
		headers: List<String>,
		rows: List<List<String>>,
		styles: TemplateStyles
	) {
		val headerRow = sheet.createRow(0)
		headers.forEachIndexed { index, header ->
			headerRow.createCell(index).apply {
				setCellValue(header)
				cellStyle = styles.header
			}
		}
		rows.forEachIndexed { rowIndex, values ->
			val row = sheet.createRow(rowIndex + 1)
			values.forEachIndexed { col, value ->
				row.createCell(col).apply {
					setCellValue(value)
					cellStyle = styles.body
				}
			}
		}
		headers.indices.forEach { sheet.autoSizeColumn(it) }
		sheet.createFreezePane(0, 1)
	}

	private fun writeGuideSheet(
		sheet: Sheet,
		styles: TemplateStyles
	) {
		val lines =
			listOf(
				"BottleNote 알코올 일괄 등록 템플릿",
				"이 시트는 설명·예제·오류 코드입니다. 실제 입력은 '알코올 데이터' 시트에만 작성하세요.",
				"",
				"[시트 구성]",
				"1. 사용 안내: 설명, 예제, 오류 코드",
				"2. 지역: ID / 한글 이름 / 영문 이름",
				"3. 증류소: ID / 한글 이름 / 영문 이름",
				"4. 테이스팅 태그: ID / 한글 이름 / 영문 이름",
				"5. 카테고리: ID / 카테고리 그룹 / 한글 카테고리 / 영문 카테고리",
				"6. 알코올 데이터: 실제 입력 시트",
				"",
				"[입력 규칙]",
				"- 1행은 헤더, 2행은 설명입니다. 두 행은 삭제하지 말고 데이터는 3행부터 입력합니다.",
				"- 주류 종류와 카테고리 그룹은 한글 표시값 또는 enum 이름을 입력합니다.",
				"- 카테고리 ID는 그룹|한글|영문 형식입니다. 카테고리 그룹은 비워 두면 ID의 그룹을 자동 사용합니다.",
				"- 지역/증류소/테이스팅 태그는 ID를 입력합니다. 참조 시트는 안내용이므로 삭제하거나 순서를 바꿔도 됩니다.",
				"- 도수는 % 표기를, 용량은 ml·cl·L 표기를 허용합니다. 숫자 셀의 퍼센트 서식도 지원합니다.",
				"- 숙성 연도, 캐스크, 설명은 선택입니다.",
				"- 테이스팅 태그 ID는 여러 개일 때 | 로 구분합니다. 예: 1|3",
				"- 파일 내부 중복과 기존 등록 후보는 경고(WARN)로 반환합니다.",
				"- 이미지는 이 템플릿에 포함되지 않습니다.",
				"- 수식 셀과 외부 링크는 허용되지 않습니다.",
				"",
				"[예제 1행]"
			)

		lines.forEachIndexed { index, line ->
			sheet.createRow(index).createCell(0).apply {
				setCellValue(line)
				cellStyle = if (index == 0) styles.title else styles.guide
			}
		}

		val exampleHeaderRowIndex = lines.size
		val exampleHeaderRow = sheet.createRow(exampleHeaderRowIndex)
		AlcoholExcelSchema.HEADERS.forEachIndexed { index, header ->
			exampleHeaderRow.createCell(index).apply {
				setCellValue(header)
				cellStyle = styles.header
			}
		}
		val exampleRow = sheet.createRow(exampleHeaderRowIndex + 1)
		AlcoholExcelSchema.EXAMPLE_ROW.forEachIndexed { index, value ->
			exampleRow.createCell(index).apply {
				setCellValue(value)
				cellStyle = styles.example
			}
		}

		val errorTitleRow = sheet.createRow(exampleHeaderRowIndex + 3)
		errorTitleRow.createCell(0).apply {
			setCellValue("[오류 코드 정의]")
			cellStyle = styles.title
		}
		val errorHeader = sheet.createRow(exampleHeaderRowIndex + 4)
		listOf("코드", "메시지 템플릿").forEachIndexed { index, header ->
			errorHeader.createCell(index).apply {
				setCellValue(header)
				cellStyle = styles.header
			}
		}
		AlcoholExcelSchema.ERROR_CATALOG.forEachIndexed { index, item ->
			val row = sheet.createRow(exampleHeaderRowIndex + 5 + index)
			row.createCell(0).apply {
				setCellValue(item.code)
				cellStyle = styles.body
			}
			row.createCell(1).apply {
				setCellValue(item.messageTemplate)
				cellStyle = styles.body
			}
		}

		(0 until maxOf(2, AlcoholExcelSchema.HEADERS.size)).forEach { sheet.autoSizeColumn(it) }
		sheet.setColumnWidth(0, sheet.getColumnWidth(0).coerceAtLeast(28 * 256))
		sheet.setColumnWidth(1, sheet.getColumnWidth(1).coerceAtLeast(60 * 256))
	}

	private fun addDropdownValidations(
		workbook: Workbook,
		dataSheet: Sheet,
		regionCount: Int,
		distilleryCount: Int,
		tagCount: Int,
		categoryCount: Int
	) {
		val helper: DataValidationHelper = dataSheet.dataValidationHelper

		fun namedRange(
			name: String,
			sheetName: String,
			columnLetter: String,
			rowCount: Int
		) {
			if (rowCount <= 0) return
			val named: Name = workbook.createName()
			named.nameName = name
			named.refersToFormula = "'$sheetName'!\$${columnLetter}\$2:\$${columnLetter}\$${rowCount + 1}"
		}

		namedRange("RegionIds", AlcoholExcelSchema.REGION_SHEET_NAME, "A", regionCount)
		namedRange("DistilleryIds", AlcoholExcelSchema.DISTILLERY_SHEET_NAME, "A", distilleryCount)
		namedRange("TagIds", AlcoholExcelSchema.TASTING_TAG_SHEET_NAME, "A", tagCount)
		namedRange("CategoryIds", AlcoholExcelSchema.CATEGORY_SHEET_NAME, "A", categoryCount)
		namedRange("CategoryGroupNames", AlcoholExcelSchema.CATEGORY_SHEET_NAME, "B", categoryCount)

		fun listValidation(
			columnIndex: Int,
			formula: String?,
			explicitList: Array<String>?
		) {
			val constraint =
				when {
					formula != null -> helper.createFormulaListConstraint(formula)
					explicitList != null -> helper.createExplicitListConstraint(explicitList)
					else -> return
				}
			val address =
				CellRangeAddressList(
					AlcoholExcelSchema.DATA_START_ROW_INDEX,
					AlcoholExcelSchema.DATA_START_ROW_INDEX + 999,
					columnIndex,
					columnIndex
				)
			val validation: DataValidation = helper.createValidation(constraint, address)
			validation.showErrorBox = true
			validation.suppressDropDownArrow = true
			dataSheet.addValidationData(validation)
		}

		listValidation(Column.TYPE.index, null, AlcoholType.entries.map { it.type }.toTypedArray())
		if (categoryCount > 0) {
			listValidation(Column.CATEGORY_ID.index, "CategoryIds", null)
			listValidation(Column.CATEGORY_GROUP.index, "CategoryGroupNames", null)
		} else {
			listValidation(Column.CATEGORY_GROUP.index, null, AlcoholCategoryGroup.entries.map { it.description }.toTypedArray())
		}
		if (regionCount > 0) listValidation(Column.REGION_ID.index, "RegionIds", null)
		if (distilleryCount > 0) listValidation(Column.DISTILLERY_ID.index, "DistilleryIds", null)
	}

	private fun isCompletelyBlank(row: Row): Boolean = AlcoholExcelSchema.HEADERS.indices.all { index -> readRawCell(row.getCell(index)).isBlank() }

	private fun readRawCell(cell: Cell?, percentageFormatted: Boolean = false): String {
		if (cell == null) return ""
		return when (cell.cellType) {
			CellType.STRING -> cell.stringCellValue?.trim().orEmpty()
			CellType.NUMERIC -> {
				val value = BigDecimal.valueOf(cell.numericCellValue).stripTrailingZeros().toPlainString()
				if (percentageFormatted && hasPercentageFormat(applicableNumberFormat(cell.cellStyle.dataFormatString, cell.numericCellValue))) {
					BigDecimal(value).multiply(BigDecimal(100)).stripTrailingZeros().toPlainString() + "%"
				} else {
					value
				}
			}
			CellType.BOOLEAN -> cell.booleanCellValue.toString()
			CellType.BLANK -> ""
			CellType.FORMULA -> throw AlcoholException(AlcoholExceptionCode.EXCEL_FORMULA_NOT_ALLOWED)
			else -> cell.toString().trim()
		}
	}

	private fun issue(
		code: String,
		field: String?,
		message: String
	) = AdminAlcoholExcelIssue(code = code, field = field, message = message)

	private fun categoryStableId(item: CategoryItem): String {
		val group = item.categoryGroup()?.name.orEmpty()
		val kor = item.korCategory().orEmpty()
		val eng = item.engCategory().orEmpty()
		return listOf(group, kor, eng).joinToString("|")
	}

	private fun loadBulkReferenceCategories(): List<CategoryItem> = alcoholQueryRepository
		.findBulkCategoryItems()
		.mapNotNull(::toCategoryItem)
		.distinctBy(::categoryStableId)

	private fun toCategoryItem(item: AlcoholBulkCategoryItem): CategoryItem? {
		val group = item.categoryGroup() ?: return null
		val korCategory = item.korCategory()?.takeIf(String::isNotBlank) ?: return null
		val engCategory = item.engCategory()?.takeIf(String::isNotBlank) ?: return null
		return CategoryItem(korCategory, engCategory, group)
	}

	private fun applicableNumberFormat(format: String?, value: Double): String? {
		if (format.isNullOrEmpty()) return format
		val sections = mutableListOf<String>()
		var start = 0
		var quoted = false
		var index = 0
		while (index < format.length) {
			when (format[index]) {
				'"' -> quoted = !quoted
				'\\' -> index++
				'_', '*' -> if (!quoted) index++
				';' -> if (!quoted) {
					sections += format.substring(start, index)
					start = index + 1
				}
			}
			index++
		}
		sections += format.substring(start)
		fun applies(section: String, fallback: Boolean): Boolean {
			val parsed = CellFormatPart.FORMAT_PAT.matcher(section)
			return if (parsed.matches() && parsed.group(CellFormatPart.CONDITION_OPERATOR_GROUP) != null) {
				CellFormatPart(section).applies(value)
			} else {
				fallback
			}
		}
		val first = sections[0]
		if (applies(first, sections.size == 1 || if (sections.size == 2) value >= 0 else value > 0)) return first
		if (sections.size == 1) return null
		val second = sections[1]
		if (applies(second, sections.size == 2 || value < 0)) return second
		return sections.getOrNull(2)
	}

	private fun hasPercentageFormat(format: String?): Boolean {
		if (format.isNullOrEmpty()) return false
		var quoted = false
		var index = 0
		while (index < format.length) {
			when (format[index]) {
				'"' -> quoted = !quoted
				'\\' -> index++
				'_', '*' -> if (!quoted) index++
				'%' -> if (!quoted) return true
			}
			index++
		}
		return false
	}

	private fun loadRegions(): List<AdminRegionItem> {
		val items = mutableListOf<AdminRegionItem>()
		var pageNumber = 0
		do {
			val page = regionRepository.findAllRegions(null, PageRequest.of(pageNumber, REFERENCE_PAGE_SIZE))
			items += page.content
			pageNumber++
		} while (page.hasNext())
		return items
	}

	private fun loadDistilleries(): List<AdminDistilleryItem> {
		val items = mutableListOf<AdminDistilleryItem>()
		var pageNumber = 0
		do {
			val page = distilleryRepository.findAllDistilleries(null, PageRequest.of(pageNumber, REFERENCE_PAGE_SIZE))
			items += page.content
			pageNumber++
		} while (page.hasNext())
		return items
	}

	private fun loadTastingTags(): List<TastingTagNodeItem> {
		val items = mutableListOf<TastingTagNodeItem>()
		var pageNumber = 0
		do {
			val page = tastingTagRepository.findAllTastingTags(null, PageRequest.of(pageNumber, REFERENCE_PAGE_SIZE))
			items += page.content
			pageNumber++
		} while (page.hasNext())
		return items
	}

	private data class ParsedRow(
		val rowNumber: Int,
		val korName: String,
		val engName: String,
		val abv: String,
		val type: String,
		val categoryId: String,
		val categoryGroup: String,
		val regionId: String,
		val distilleryId: String,
		val age: String,
		val cask: String,
		val description: String,
		val volume: String,
		val tastingTagIds: String
	)

	private data class BulkAdapterRow(
		val parsed: ParsedRow,
		val request: AdminAlcoholBulkRowRequest,
		val errors: List<AdminAlcoholExcelIssue>,
		val warnings: List<AdminAlcoholExcelIssue>
	)

	private class TemplateStyles(
		workbook: Workbook
	) {
		val title: CellStyle =
			workbook.createCellStyle().apply {
				setFont(
					workbook.createFont().apply {
						bold = true
						fontHeightInPoints = 14
						color = IndexedColors.DARK_BLUE.index
					}
				)
				verticalAlignment = VerticalAlignment.CENTER
			}
		val header: CellStyle =
			workbook.createCellStyle().apply {
				fillForegroundColor = IndexedColors.DARK_BLUE.index
				fillPattern = FillPatternType.SOLID_FOREGROUND
				alignment = HorizontalAlignment.CENTER
				verticalAlignment = VerticalAlignment.CENTER
				borderBottom = BorderStyle.THIN
				borderTop = BorderStyle.THIN
				borderLeft = BorderStyle.THIN
				borderRight = BorderStyle.THIN
				setFont(
					workbook.createFont().apply {
						bold = true
						color = IndexedColors.WHITE.index
						fontHeightInPoints = 11
					}
				)
			}
		val description: CellStyle =
			workbook.createCellStyle().apply {
				fillForegroundColor = IndexedColors.LIGHT_CORNFLOWER_BLUE.index
				fillPattern = FillPatternType.SOLID_FOREGROUND
				wrapText = true
				verticalAlignment = VerticalAlignment.CENTER
				borderBottom = BorderStyle.THIN
				borderTop = BorderStyle.THIN
				borderLeft = BorderStyle.THIN
				borderRight = BorderStyle.THIN
				setFont(
					workbook.createFont().apply {
						italic = true
						fontHeightInPoints = 10
						color = IndexedColors.DARK_BLUE.index
					}
				)
			}
		val body: CellStyle =
			workbook.createCellStyle().apply {
				verticalAlignment = VerticalAlignment.CENTER
				borderBottom = BorderStyle.THIN
				borderTop = BorderStyle.THIN
				borderLeft = BorderStyle.THIN
				borderRight = BorderStyle.THIN
			}
		val example: CellStyle =
			workbook.createCellStyle().apply {
				fillForegroundColor = IndexedColors.LIGHT_YELLOW.index
				fillPattern = FillPatternType.SOLID_FOREGROUND
				verticalAlignment = VerticalAlignment.CENTER
				borderBottom = BorderStyle.THIN
				borderTop = BorderStyle.THIN
				borderLeft = BorderStyle.THIN
				borderRight = BorderStyle.THIN
			}
		val guide: CellStyle =
			workbook.createCellStyle().apply {
				wrapText = true
				verticalAlignment = VerticalAlignment.CENTER
			}
	}
}
