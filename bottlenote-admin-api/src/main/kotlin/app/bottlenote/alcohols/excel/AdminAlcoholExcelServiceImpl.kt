package app.bottlenote.alcohols.excel

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup
import app.bottlenote.alcohols.constant.AlcoholType
import app.bottlenote.alcohols.domain.Alcohol
import app.bottlenote.alcohols.domain.AlcoholQueryRepository
import app.bottlenote.alcohols.domain.DistilleryRepository
import app.bottlenote.alcohols.domain.RegionRepository
import app.bottlenote.alcohols.domain.TastingTagRepository
import app.bottlenote.alcohols.dto.response.AdminAlcoholExcelIssue
import app.bottlenote.alcohols.dto.response.AdminAlcoholExcelRowResult
import app.bottlenote.alcohols.dto.response.AdminAlcoholExcelValidateResponse
import app.bottlenote.alcohols.excel.AlcoholExcelSchema.Column
import app.bottlenote.alcohols.exception.AlcoholException
import app.bottlenote.alcohols.exception.AlcoholExceptionCode
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataValidation
import org.apache.poi.ss.usermodel.DataValidationHelper
import org.apache.poi.ss.usermodel.Name
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.Normalizer
import java.util.Locale

@Service
class AdminAlcoholExcelServiceImpl(
	private val regionRepository: RegionRepository,
	private val distilleryRepository: DistilleryRepository,
	private val tastingTagRepository: TastingTagRepository,
	private val alcoholQueryRepository: AlcoholQueryRepository,
) : AdminAlcoholExcelService {
	override fun createTemplateWorkbook(): ByteArray {
		XSSFWorkbook().use { workbook ->
			val dataSheet = workbook.createSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
			val regionSheet = workbook.createSheet(AlcoholExcelSchema.REGION_SHEET_NAME)
			val distillerySheet = workbook.createSheet(AlcoholExcelSchema.DISTILLERY_SHEET_NAME)
			val tagSheet = workbook.createSheet(AlcoholExcelSchema.TASTING_TAG_SHEET_NAME)
			val guideSheet = workbook.createSheet(AlcoholExcelSchema.GUIDE_SHEET_NAME)

			writeHeaderAndDescription(dataSheet)
			writeReferenceSheet(
				regionSheet,
				listOf("한글 이름", "영문 이름"),
				regionRepository.findAllOrderBySortOrderAsc().map { listOf(it.korName.orEmpty(), it.engName.orEmpty()) },
			)
			writeReferenceSheet(
				distillerySheet,
				listOf("한글 이름", "영문 이름"),
				distilleryRepository.findAllOrderBySortOrderAsc().map { listOf(it.korName.orEmpty(), it.engName.orEmpty()) },
			)
			writeReferenceSheet(
				tagSheet,
				listOf("한글 이름", "영문 이름"),
				tastingTagRepository.findAll().map { listOf(it.korName.orEmpty(), it.engName.orEmpty()) },
			)
			writeGuideSheet(guideSheet)
			addDropdownValidations(workbook, dataSheet)

			AlcoholExcelSchema.HEADERS.indices.forEach { dataSheet.autoSizeColumn(it) }

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

			rejectFormulas(dataSheet)

			val regionsByKorName =
				regionRepository.findAllOrderBySortOrderAsc().associateBy { normalizeExact(it.korName) }
			val distilleriesByKorName =
				distilleryRepository.findAllOrderBySortOrderAsc().associateBy { normalizeExact(it.korName) }
			val tagsByKorName = tastingTagRepository.findAll().associateBy { normalizeExact(it.korName) }
			val existingAlcohols =
				alcoholQueryRepository.findAll().filter { it.deletedAt == null }

			val parsedRows = mutableListOf<ParsedRow>()
			val lastRow = dataSheet.lastRowNum
			for (rowIndex in AlcoholExcelSchema.DATA_START_ROW_INDEX..lastRow) {
				val row = dataSheet.getRow(rowIndex) ?: continue
				if (isCompletelyBlank(row)) {
					continue
				}
				parsedRows += parseRow(rowIndex, row)
			}

			if (parsedRows.size > AlcoholExcelSchema.MAX_DATA_ROWS) {
				throw AlcoholException(AlcoholExceptionCode.EXCEL_ROW_LIMIT_EXCEEDED)
			}

			val identityCounts = parsedRows.groupingBy { it.identityKey }.eachCount()
			val results =
				parsedRows.map { parsed ->
					validateParsedRow(
						parsed,
						regionsByKorName,
						distilleriesByKorName,
						tagsByKorName,
						existingAlcohols,
						identityCounts,
					)
				}

			return AdminAlcoholExcelValidateResponse(
				totalRows = results.size,
				validRows = results.count { it.valid },
				invalidRows = results.count { !it.valid },
				warningRows = results.count { it.warnings.isNotEmpty() },
				rows = results,
			)
		}
	}

	private fun validateFileEnvelope(file: MultipartFile) {
		if (file.isEmpty) {
			throw AlcoholException(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE)
		}
		if (file.size > AlcoholExcelSchema.MAX_FILE_BYTES) {
			throw AlcoholException(AlcoholExceptionCode.EXCEL_FILE_TOO_LARGE)
		}
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
		try {
			// 바이트 배열 기반 OOXML만 허용. 수식 셀은 별도 검사에서 거부한다.
			return XSSFWorkbook(ByteArrayInputStream(bytes))
		} catch (_: Exception) {
			throw AlcoholException(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE)
		}
	}

	private fun validateWorkbookStructure(workbook: Workbook) {
		val requiredSheets =
			listOf(
				AlcoholExcelSchema.DATA_SHEET_NAME,
				AlcoholExcelSchema.REGION_SHEET_NAME,
				AlcoholExcelSchema.DISTILLERY_SHEET_NAME,
				AlcoholExcelSchema.TASTING_TAG_SHEET_NAME,
				AlcoholExcelSchema.GUIDE_SHEET_NAME,
			)
		requiredSheets.forEach { name ->
			if (workbook.getSheet(name) == null) {
				throw AlcoholException(AlcoholExceptionCode.EXCEL_SHEET_NOT_FOUND)
			}
		}

		val dataSheet = workbook.getSheet(AlcoholExcelSchema.DATA_SHEET_NAME)
		val headerRow =
			dataSheet.getRow(AlcoholExcelSchema.HEADER_ROW_INDEX)
				?: throw AlcoholException(AlcoholExceptionCode.EXCEL_HEADER_MISMATCH)
		val descriptionRow =
			dataSheet.getRow(AlcoholExcelSchema.DESCRIPTION_ROW_INDEX)
				?: throw AlcoholException(AlcoholExceptionCode.EXCEL_DESCRIPTION_MISMATCH)

		val headers = AlcoholExcelSchema.HEADERS.indices.map { readRawCell(headerRow.getCell(it)) }
		if (headers != AlcoholExcelSchema.HEADERS) {
			if (headers.size != headers.distinct().size) {
				throw AlcoholException(AlcoholExceptionCode.EXCEL_DUPLICATE_HEADER)
			}
			throw AlcoholException(AlcoholExceptionCode.EXCEL_HEADER_MISMATCH)
		}

		val descriptions = AlcoholExcelSchema.DESCRIPTIONS.indices.map { readRawCell(descriptionRow.getCell(it)) }
		if (descriptions != AlcoholExcelSchema.DESCRIPTIONS) {
			throw AlcoholException(AlcoholExceptionCode.EXCEL_DESCRIPTION_MISMATCH)
		}
	}

	private fun rejectFormulas(sheet: Sheet) {
		for (rowIndex in 0..sheet.lastRowNum) {
			val row = sheet.getRow(rowIndex) ?: continue
			for (cellIndex in 0 until row.lastCellNum.coerceAtLeast(0)) {
				val cell = row.getCell(cellIndex) ?: continue
				if (cell.cellType == CellType.FORMULA || cell.cellType == CellType.ERROR) {
					throw AlcoholException(AlcoholExceptionCode.EXCEL_FORMULA_NOT_ALLOWED)
				}
			}
		}
	}

	private fun parseRow(
		rowIndex: Int,
		row: Row,
	): ParsedRow {
		fun cell(column: Column): String = readRawCell(row.getCell(column.index))

		return ParsedRow(
			rowNumber = rowIndex + 1,
			korName = cell(Column.KOR_NAME),
			engName = cell(Column.ENG_NAME),
			abv = cell(Column.ABV),
			type = cell(Column.TYPE),
			korCategory = cell(Column.KOR_CATEGORY),
			engCategory = cell(Column.ENG_CATEGORY),
			categoryGroup = cell(Column.CATEGORY_GROUP),
			region = cell(Column.REGION),
			distillery = cell(Column.DISTILLERY),
			age = cell(Column.AGE),
			cask = cell(Column.CASK),
			description = cell(Column.DESCRIPTION),
			volume = cell(Column.VOLUME),
			tastingTags = cell(Column.TASTING_TAGS),
		)
	}

	private fun validateParsedRow(
		parsed: ParsedRow,
		regionsByKorName: Map<String, app.bottlenote.alcohols.domain.Region>,
		distilleriesByKorName: Map<String, app.bottlenote.alcohols.domain.Distillery>,
		tagsByKorName: Map<String, app.bottlenote.alcohols.domain.TastingTag>,
		existingAlcohols: List<Alcohol>,
		identityCounts: Map<IdentityKey, Int>,
	): AdminAlcoholExcelRowResult {
		val errors = mutableListOf<AdminAlcoholExcelIssue>()
		val warnings = mutableListOf<AdminAlcoholExcelIssue>()

		fun requireValue(
			value: String,
			column: Column,
		): String? {
			if (value.isBlank()) {
				errors +=
					issue(
						"REQUIRED_FIELD",
						column.header,
						"${column.header}은(는) 필수입니다.",
					)
				return null
			}
			return value
		}

		val korName = requireValue(parsed.korName, Column.KOR_NAME)
		val engName = requireValue(parsed.engName, Column.ENG_NAME)
		val abv = requireValue(parsed.abv, Column.ABV)
		val typeRaw = requireValue(parsed.type, Column.TYPE)
		val korCategory = requireValue(parsed.korCategory, Column.KOR_CATEGORY)
		val engCategory = requireValue(parsed.engCategory, Column.ENG_CATEGORY)
		val categoryGroupRaw = requireValue(parsed.categoryGroup, Column.CATEGORY_GROUP)
		val regionRaw = requireValue(parsed.region, Column.REGION)
		val distilleryRaw = requireValue(parsed.distillery, Column.DISTILLERY)
		val age = requireValue(parsed.age, Column.AGE)
		val cask = requireValue(parsed.cask, Column.CASK)
		val description = requireValue(parsed.description, Column.DESCRIPTION)
		val volume = requireValue(parsed.volume, Column.VOLUME)

		var typeName: String? = null
		if (typeRaw != null) {
			val matchedType = AlcoholType.entries.firstOrNull { it.type == typeRaw.trim() || it.name == typeRaw.trim().uppercase(Locale.ROOT) }
			if (matchedType == null) {
				errors += issue("INVALID_ENUM_VALUE", Column.TYPE.header, "지원하지 않는 주류 종류입니다: $typeRaw")
			} else {
				typeName = matchedType.name
			}
		}

		var categoryGroupName: String? = null
		if (categoryGroupRaw != null) {
			val matchedGroup =
				AlcoholCategoryGroup.entries.firstOrNull {
					it.description == categoryGroupRaw.trim() || it.name == categoryGroupRaw.trim().uppercase(Locale.ROOT)
				}
			if (matchedGroup == null) {
				errors +=
					issue(
						"INVALID_ENUM_VALUE",
						Column.CATEGORY_GROUP.header,
						"지원하지 않는 카테고리 그룹입니다: $categoryGroupRaw",
					)
			} else {
				categoryGroupName = matchedGroup.name
			}
		}

		var regionId: Long? = null
		if (regionRaw != null) {
			val region = regionsByKorName[normalizeExact(regionRaw)]
			if (region == null) {
				errors += issue("REGION_NOT_FOUND", Column.REGION.header, "지역을 찾을 수 없습니다: $regionRaw")
			} else {
				regionId = region.id
			}
		}

		var distilleryId: Long? = null
		var matchedDistilleryName: String? = null
		if (distilleryRaw != null) {
			val distillery = distilleriesByKorName[normalizeExact(distilleryRaw)]
			if (distillery == null) {
				errors +=
					issue(
						"DISTILLERY_NOT_FOUND",
						Column.DISTILLERY.header,
						"증류소를 찾을 수 없습니다: $distilleryRaw",
					)
			} else {
				distilleryId = distillery.id
				matchedDistilleryName = distillery.korName
			}
		}

		val tastingTagIds = mutableListOf<Long>()
		if (parsed.tastingTags.isNotBlank()) {
			val tagNames =
				parsed.tastingTags
					.split("|")
					.map { it.trim() }
					.filter { it.isNotEmpty() }
			val seen = mutableSetOf<String>()
			tagNames.forEach { tagName ->
				val key = normalizeExact(tagName)
				if (!seen.add(key)) {
					errors +=
						issue(
							"DUPLICATE_TASTING_TAG",
							Column.TASTING_TAGS.header,
							"중복된 테이스팅 태그입니다: $tagName",
						)
					return@forEach
				}
				val tag = tagsByKorName[key]
				if (tag == null) {
					errors +=
						issue(
							"TASTING_TAG_NOT_FOUND",
							Column.TASTING_TAGS.header,
							"테이스팅 태그를 찾을 수 없습니다: $tagName",
						)
				} else {
					tastingTagIds += tag.id
				}
			}
		}

		if (identityCounts[parsed.identityKey]?.let { it > 1 } == true) {
			errors +=
				issue(
					"DUPLICATE_IN_FILE",
					null,
					"파일 내부에 동일한 식별 조합(이름·증류소·도수·용량)이 중복됩니다.",
				)
		}

		val candidateIds =
			if (korName != null && matchedDistilleryName != null && abv != null && volume != null) {
				existingAlcohols
					.filter { alcohol ->
						normalizeIdentity(alcohol.korName) == normalizeIdentity(korName) &&
							normalizeIdentity(alcohol.distillery?.korName) == normalizeIdentity(matchedDistilleryName) &&
							normalizeIdentity(alcohol.abv) == normalizeIdentity(abv) &&
							normalizeIdentity(alcohol.volume) == normalizeIdentity(volume)
					}.mapNotNull { it.id }
			} else {
				emptyList()
			}

		if (candidateIds.isNotEmpty()) {
			warnings +=
				issue(
					"DUPLICATE_CANDIDATE",
					null,
					"기존 등록 알코올과 강하게 일치합니다. 후보 ID: ${candidateIds.joinToString(", ")}",
				)
		}

		return AdminAlcoholExcelRowResult(
			rowNumber = parsed.rowNumber,
			korName = korName ?: parsed.korName.ifBlank { null },
			engName = engName ?: parsed.engName.ifBlank { null },
			abv = abv ?: parsed.abv.ifBlank { null },
			type = typeName ?: typeRaw,
			korCategory = korCategory ?: parsed.korCategory.ifBlank { null },
			engCategory = engCategory ?: parsed.engCategory.ifBlank { null },
			categoryGroup = categoryGroupName ?: categoryGroupRaw,
			region = regionRaw ?: parsed.region.ifBlank { null },
			distillery = distilleryRaw ?: parsed.distillery.ifBlank { null },
			age = age ?: parsed.age.ifBlank { null },
			cask = cask ?: parsed.cask.ifBlank { null },
			description = description ?: parsed.description.ifBlank { null },
			volume = volume ?: parsed.volume.ifBlank { null },
			tastingTags = parsed.tastingTags.ifBlank { null },
			regionId = regionId,
			distilleryId = distilleryId,
			tastingTagIds = tastingTagIds.takeIf { it.isNotEmpty() },
			candidateAlcoholIds = candidateIds.takeIf { it.isNotEmpty() },
			valid = errors.isEmpty(),
			errors = errors,
			warnings = warnings,
		)
	}

	private fun writeHeaderAndDescription(sheet: Sheet) {
		val headerRow = sheet.createRow(AlcoholExcelSchema.HEADER_ROW_INDEX)
		val descriptionRow = sheet.createRow(AlcoholExcelSchema.DESCRIPTION_ROW_INDEX)
		AlcoholExcelSchema.HEADERS.forEachIndexed { index, header ->
			headerRow.createCell(index).setCellValue(header)
			descriptionRow.createCell(index).setCellValue(AlcoholExcelSchema.DESCRIPTIONS[index])
		}
	}

	private fun writeReferenceSheet(
		sheet: Sheet,
		headers: List<String>,
		rows: List<List<String>>,
	) {
		val headerRow = sheet.createRow(0)
		headers.forEachIndexed { index, header -> headerRow.createCell(index).setCellValue(header) }
		rows.forEachIndexed { rowIndex, values ->
			val row = sheet.createRow(rowIndex + 1)
			values.forEachIndexed { col, value -> row.createCell(col).setCellValue(value) }
		}
		headers.indices.forEach { sheet.autoSizeColumn(it) }
	}

	private fun writeGuideSheet(sheet: Sheet) {
		val lines =
			listOf(
				"입력 안내",
				"1. '알코올 데이터' 시트의 1행(필드명)과 2행(설명)은 수정하지 마세요.",
				"2. 3행부터 데이터를 입력합니다. 완전히 빈 행은 무시됩니다.",
				"3. 주류 종류/카테고리 그룹은 한글 표시값을 입력합니다.",
				"4. 지역·증류소·테이스팅 태그는 각 참조 시트의 한글 이름과 정확히 일치해야 합니다.",
				"5. 테이스팅 태그는 여러 개일 때 | 로 구분합니다. 예: 오크|피트",
				"6. 이미지는 이 템플릿에 포함되지 않습니다. 단건 등록/수정 API에서 별도 처리합니다.",
				"7. 수식 셀과 외부 링크는 허용되지 않습니다.",
				"예시) 한글 이름=글렌피딕 12년 / 영문 이름=Glenfiddich 12 / 도수=40% / 주류 종류=위스키 / 카테고리 그룹=싱글몰트 위스키",
			)
		lines.forEachIndexed { index, line ->
			sheet.createRow(index).createCell(0).setCellValue(line)
		}
		sheet.autoSizeColumn(0)
	}

	private fun addDropdownValidations(
		workbook: Workbook,
		dataSheet: Sheet,
	) {
		val helper: DataValidationHelper = dataSheet.dataValidationHelper

		fun namedRange(
			name: String,
			sheetName: String,
			rowCount: Int,
		) {
			if (rowCount <= 0) return
			val named: Name = workbook.createName()
			named.nameName = name
			named.refersToFormula = "'$sheetName'!\$A\$2:\$A\$${rowCount + 1}"
		}

		val regionCount = regionRepository.findAllOrderBySortOrderAsc().size
		val distilleryCount = distilleryRepository.findAllOrderBySortOrderAsc().size
		val tagCount = tastingTagRepository.findAll().size

		namedRange("RegionNames", AlcoholExcelSchema.REGION_SHEET_NAME, regionCount)
		namedRange("DistilleryNames", AlcoholExcelSchema.DISTILLERY_SHEET_NAME, distilleryCount)
		namedRange("TagNames", AlcoholExcelSchema.TASTING_TAG_SHEET_NAME, tagCount)

		fun listValidation(
			columnIndex: Int,
			formula: String?,
			explicitList: Array<String>?,
		) {
			val constraint =
				when {
					formula != null -> helper.createFormulaListConstraint(formula)
					explicitList != null -> helper.createExplicitListConstraint(explicitList)
					else -> return
				}
			val address = CellRangeAddressList(AlcoholExcelSchema.DATA_START_ROW_INDEX, AlcoholExcelSchema.DATA_START_ROW_INDEX + 999, columnIndex, columnIndex)
			val validation: DataValidation = helper.createValidation(constraint, address)
			validation.showErrorBox = true
			validation.suppressDropDownArrow = true
			dataSheet.addValidationData(validation)
		}

		listValidation(
			Column.TYPE.index,
			null,
			AlcoholType.entries.map { it.type }.toTypedArray(),
		)
		listValidation(
			Column.CATEGORY_GROUP.index,
			null,
			AlcoholCategoryGroup.entries.map { it.description }.toTypedArray(),
		)
		if (regionCount > 0) {
			listValidation(Column.REGION.index, "RegionNames", null)
		}
		if (distilleryCount > 0) {
			listValidation(Column.DISTILLERY.index, "DistilleryNames", null)
		}
	}

	private fun isCompletelyBlank(row: Row): Boolean =
		AlcoholExcelSchema.HEADERS.indices.all { index ->
			readRawCell(row.getCell(index)).isBlank()
		}

	private fun readRawCell(cell: Cell?): String {
		if (cell == null) return ""
		return when (cell.cellType) {
			CellType.STRING -> cell.stringCellValue?.trim().orEmpty()
			CellType.NUMERIC -> {
				val value = cell.numericCellValue
				if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
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
		message: String,
	) = AdminAlcoholExcelIssue(code = code, field = field, message = message)

	private fun normalizeExact(value: String?): String = normalizeIdentity(value)

	private fun normalizeIdentity(value: String?): String {
		if (value.isNullOrBlank()) return ""
		val nfkc = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
		return nfkc
			.lowercase(Locale.ROOT)
			.replace(Regex("\\s+"), " ")
	}

	private data class ParsedRow(
		val rowNumber: Int,
		val korName: String,
		val engName: String,
		val abv: String,
		val type: String,
		val korCategory: String,
		val engCategory: String,
		val categoryGroup: String,
		val region: String,
		val distillery: String,
		val age: String,
		val cask: String,
		val description: String,
		val volume: String,
		val tastingTags: String,
	) {
		val identityKey: IdentityKey
			get() =
				IdentityKey(
					normalizeIdentity(korName),
					normalizeIdentity(distillery),
					normalizeIdentity(abv),
					normalizeIdentity(volume),
				)
	}

	private data class IdentityKey(
		val name: String,
		val distillery: String,
		val abv: String,
		val volume: String,
	)
}
