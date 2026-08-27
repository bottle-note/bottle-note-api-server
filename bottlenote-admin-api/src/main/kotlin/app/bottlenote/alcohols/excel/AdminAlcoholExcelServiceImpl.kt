package app.bottlenote.alcohols.excel

import app.bottlenote.alcohols.constant.AlcoholCategoryGroup
import app.bottlenote.alcohols.constant.AlcoholType
import app.bottlenote.alcohols.domain.AlcoholQueryRepository
import app.bottlenote.alcohols.domain.DistilleryRepository
import app.bottlenote.alcohols.domain.RegionRepository
import app.bottlenote.alcohols.domain.TastingTagRepository
import app.bottlenote.alcohols.dto.response.AdminAlcoholExcelIssue
import app.bottlenote.alcohols.dto.response.AdminAlcoholExcelRowResult
import app.bottlenote.alcohols.dto.response.AdminAlcoholExcelValidateResponse
import app.bottlenote.alcohols.dto.response.AdminDistilleryItem
import app.bottlenote.alcohols.dto.response.AdminRegionItem
import app.bottlenote.alcohols.dto.response.CategoryItem
import app.bottlenote.alcohols.dto.response.TastingTagNodeItem
import app.bottlenote.alcohols.excel.AlcoholExcelSchema.Column
import app.bottlenote.alcohols.exception.AlcoholException
import app.bottlenote.alcohols.exception.AlcoholExceptionCode
import app.bottlenote.alcohols.facade.payload.AlcoholMatchTargetItem
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.openxml4j.util.ZipSecureFile
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataValidation
import org.apache.poi.ss.usermodel.DataValidationHelper
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.Font
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
import java.math.BigDecimal
import java.math.RoundingMode
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
			val categories = alcoholQueryRepository.findAllCategoryItems()

			writeGuideSheet(guideSheet, styles)
			writeReferenceSheet(
				regionSheet,
				listOf("ID", "한글 이름", "영문 이름"),
				regions.map { listOf(it.id.toString(), it.korName.orEmpty(), it.engName.orEmpty()) },
				styles,
			)
			writeReferenceSheet(
				distillerySheet,
				listOf("ID", "한글 이름", "영문 이름"),
				distilleries.map { listOf(it.id.toString(), it.korName.orEmpty(), it.engName.orEmpty()) },
				styles,
			)
			writeReferenceSheet(
				tagSheet,
				listOf("ID", "한글 이름", "영문 이름"),
				tags.map { listOf(it.id.toString(), it.korName.orEmpty(), it.engName.orEmpty()) },
				styles,
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
						item.engCategory().orEmpty(),
					)
				},
				styles,
			)
			writeHeaderAndDescription(dataSheet, styles)
			addDropdownValidations(
				workbook = workbook,
				dataSheet = dataSheet,
				regionCount = regions.size,
				distilleryCount = distilleries.size,
				tagCount = tags.size,
				categoryCount = categories.size,
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

			rejectFormulas(dataSheet)

			val regionsById = loadRegions().associateBy { it.id }
			val distilleriesById = loadDistilleries().associateBy { it.id }
			val tagsById = loadTastingTags().associateBy { it.id }
			val categories = alcoholQueryRepository.findAllCategoryItems()
			val categoriesById = categories.associateBy { categoryStableId(it) }
			// 전체 Alcohol 엔티티 대신 매칭용 요약 projection만 적재한다.
			val existingTargets = alcoholQueryRepository.findAllMatchTargets()

			val parsedRows = mutableListOf<ParsedRow>()
			val lastRow = dataSheet.lastRowNum
			for (rowIndex in AlcoholExcelSchema.DATA_START_ROW_INDEX..lastRow) {
				val row = dataSheet.getRow(rowIndex) ?: continue
				if (isCompletelyBlank(row)) continue
				parsedRows += parseRow(rowIndex, row)
			}

			if (parsedRows.size > AlcoholExcelSchema.MAX_DATA_ROWS) {
				throw AlcoholException(AlcoholExceptionCode.EXCEL_ROW_LIMIT_EXCEEDED)
			}

			// 파일 내부 중복은 정규화된 숫자 기준으로 판정한다.
			val identityCounts =
				parsedRows
					.map { it to normalizedIdentityKey(it) }
					.groupingBy { it.second }
					.eachCount()
			val results =
				parsedRows.map { parsed ->
					validateParsedRow(
						parsed = parsed,
						regionsById = regionsById,
						distilleriesById = distilleriesById,
						tagsById = tagsById,
						categoriesById = categoriesById,
						existingTargets = existingTargets,
						identityCounts = identityCounts,
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
		val previousMinInflateRatio = ZipSecureFile.getMinInflateRatio()
		val previousMaxEntrySize = ZipSecureFile.getMaxEntrySize()
		return try {
			// zip-bomb 방어: 과도 압축 OOXML 거부
			ZipSecureFile.setMinInflateRatio(0.01)
			ZipSecureFile.setMaxEntrySize(AlcoholExcelSchema.MAX_FILE_BYTES)
			val pkg = OPCPackage.open(ByteArrayInputStream(bytes))
			XSSFWorkbook(pkg)
		} catch (_: Exception) {
			throw AlcoholException(AlcoholExceptionCode.EXCEL_INVALID_FILE_TYPE)
		} finally {
			ZipSecureFile.setMinInflateRatio(previousMinInflateRatio)
			ZipSecureFile.setMaxEntrySize(previousMaxEntrySize)
		}
	}

	private fun validateWorkbookStructure(workbook: Workbook) {
		AlcoholExcelSchema.SHEET_ORDER.forEach { name ->
			if (workbook.getSheet(name) == null) throw AlcoholException(AlcoholExceptionCode.EXCEL_SHEET_NOT_FOUND)
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
			if (headers.size != headers.distinct().size) throw AlcoholException(AlcoholExceptionCode.EXCEL_DUPLICATE_HEADER)
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
			categoryId = cell(Column.CATEGORY_ID),
			categoryGroup = cell(Column.CATEGORY_GROUP),
			regionId = cell(Column.REGION_ID),
			distilleryId = cell(Column.DISTILLERY_ID),
			age = cell(Column.AGE),
			cask = cell(Column.CASK),
			description = cell(Column.DESCRIPTION),
			volume = cell(Column.VOLUME),
			tastingTagIds = cell(Column.TASTING_TAG_IDS),
		)
	}

	private fun validateParsedRow(
		parsed: ParsedRow,
		regionsById: Map<Long, AdminRegionItem>,
		distilleriesById: Map<Long, AdminDistilleryItem>,
		tagsById: Map<Long, TastingTagNodeItem>,
		categoriesById: Map<String, CategoryItem>,
		existingTargets: List<AlcoholMatchTargetItem>,
		identityCounts: Map<IdentityKey, Int>,
	): AdminAlcoholExcelRowResult {
		val errors = mutableListOf<AdminAlcoholExcelIssue>()
		val warnings = mutableListOf<AdminAlcoholExcelIssue>()

		fun requireValue(
			value: String,
			column: Column,
		): String? {
			if (value.isBlank()) {
				errors += issue("REQUIRED_FIELD", column.header, "${column.header} 필드가 누락되었거나 비어 있습니다.")
				return null
			}
			return value
		}

		val korName = requireValue(parsed.korName, Column.KOR_NAME)
		val engName = requireValue(parsed.engName, Column.ENG_NAME)
		val abvRaw = requireValue(parsed.abv, Column.ABV)
		val typeRaw = requireValue(parsed.type, Column.TYPE)
		val categoryIdRaw = requireValue(parsed.categoryId, Column.CATEGORY_ID)
		val categoryGroupRaw = requireValue(parsed.categoryGroup, Column.CATEGORY_GROUP)
		val regionIdRaw = requireValue(parsed.regionId, Column.REGION_ID)
		val distilleryIdRaw = requireValue(parsed.distilleryId, Column.DISTILLERY_ID)
		val age = requireValue(parsed.age, Column.AGE)
		val cask = requireValue(parsed.cask, Column.CASK)
		val description = requireValue(parsed.description, Column.DESCRIPTION)
		val volumeRaw = requireValue(parsed.volume, Column.VOLUME)

		val abvNormalized = abvRaw?.let { parseDecimal(it, Column.ABV, errors) }
		val volumeNormalized = volumeRaw?.let { parseDecimal(it, Column.VOLUME, errors) }
		val abvDisplay = abvNormalized?.let { formatWithSuffix(it, "%") }
		val volumeDisplay = volumeNormalized?.let { formatWithSuffix(it, "ml") }

		var typeName: String? = null
		if (typeRaw != null) {
			val matchedType =
				AlcoholType.entries.firstOrNull {
					it.type == typeRaw.trim() || it.name == typeRaw.trim().uppercase(Locale.ROOT)
				}
			if (matchedType == null) {
				errors += issue("INVALID_ENUM_VALUE", Column.TYPE.header, "${Column.TYPE.header} 필드가 잘못 입력되었습니다. 허용된 한글 값을 입력하세요.")
			} else {
				typeName = matchedType.name
			}
		}

		var categoryGroupName: String? = null
		var matchedCategoryGroup: AlcoholCategoryGroup? = null
		if (categoryGroupRaw != null) {
			matchedCategoryGroup =
				AlcoholCategoryGroup.entries.firstOrNull {
					it.description == categoryGroupRaw.trim() || it.name == categoryGroupRaw.trim().uppercase(Locale.ROOT)
				}
			if (matchedCategoryGroup == null) {
				errors +=
					issue(
						"INVALID_ENUM_VALUE",
						Column.CATEGORY_GROUP.header,
						"${Column.CATEGORY_GROUP.header} 필드가 잘못 입력되었습니다. 허용된 한글 값을 입력하세요.",
					)
			} else {
				categoryGroupName = matchedCategoryGroup.name
			}
		}

		var korCategory: String? = null
		var engCategory: String? = null
		if (categoryIdRaw != null) {
			val stableId = categoryIdRaw.trim()
			if (stableId.isBlank()) {
				errors += issue("INVALID_ID", Column.CATEGORY_ID.header, "${Column.CATEGORY_ID.header} 필드가 잘못 입력되었습니다. 참조 시트의 ID를 입력하세요.")
			} else {
				val category = categoriesById[stableId]
				if (category == null) {
					errors += issue("CATEGORY_NOT_FOUND", Column.CATEGORY_ID.header, "카테고리 ID를 찾을 수 없습니다: $stableId")
				} else {
					korCategory = category.korCategory()
					engCategory = category.engCategory()
					if (matchedCategoryGroup != null && category.categoryGroup() != matchedCategoryGroup) {
						errors +=
							issue(
								"CATEGORY_GROUP_MISMATCH",
								Column.CATEGORY_GROUP.header,
								"카테고리 ID와 카테고리 그룹이 일치하지 않습니다: ID=$stableId, 그룹=${matchedCategoryGroup.description}",
							)
					} else if (matchedCategoryGroup == null && category.categoryGroup() != null) {
						matchedCategoryGroup = category.categoryGroup()
						categoryGroupName = matchedCategoryGroup?.name
					}
				}
			}
		}

		var regionId: Long? = null
		var regionName: String? = null
		if (regionIdRaw != null) {
			val parsedId = parseLongId(regionIdRaw, Column.REGION_ID, errors)
			if (parsedId != null) {
				val region = regionsById[parsedId]
				if (region == null) {
					errors += issue("REGION_NOT_FOUND", Column.REGION_ID.header, "지역 ID를 찾을 수 없습니다: $parsedId")
				} else {
					regionId = parsedId
					regionName = region.korName()
				}
			}
		}

		var distilleryId: Long? = null
		var distilleryName: String? = null
		if (distilleryIdRaw != null) {
			val parsedId = parseLongId(distilleryIdRaw, Column.DISTILLERY_ID, errors)
			if (parsedId != null) {
				val distillery = distilleriesById[parsedId]
				if (distillery == null) {
					errors += issue("DISTILLERY_NOT_FOUND", Column.DISTILLERY_ID.header, "증류소 ID를 찾을 수 없습니다: $parsedId")
				} else {
					distilleryId = parsedId
					distilleryName = distillery.korName()
				}
			}
		}

		val tastingTagIds = mutableListOf<Long>()
		if (parsed.tastingTagIds.isNotBlank()) {
			val rawIds =
				parsed.tastingTagIds
					.split("|")
					.map { it.trim() }
					.filter { it.isNotEmpty() }
			val seen = mutableSetOf<Long>()
			rawIds.forEach { raw ->
				val id = parseLongId(raw, Column.TASTING_TAG_IDS, errors) ?: return@forEach
				if (!seen.add(id)) {
					errors += issue("DUPLICATE_TASTING_TAG", Column.TASTING_TAG_IDS.header, "중복된 테이스팅 태그 ID입니다: $id")
					return@forEach
				}
				val tag = tagsById[id]
				if (tag == null) {
					errors += issue("TASTING_TAG_NOT_FOUND", Column.TASTING_TAG_IDS.header, "테이스팅 태그 ID를 찾을 수 없습니다: $id")
				} else {
					tastingTagIds += id
				}
			}
		}

		if (identityCounts[normalizedIdentityKey(parsed)]?.let { it > 1 } == true) {
			errors +=
				issue(
					"DUPLICATE_IN_FILE",
					null,
					"파일 내부에 동일한 식별 조합(이름·증류소·도수·용량)이 중복됩니다: ${parsed.korName}/${parsed.distilleryId}/${parsed.abv}/${parsed.volume}",
				)
		}

		val candidateIds =
			if (korName != null && distilleryId != null && abvNormalized != null) {
				existingTargets
					.filter { target ->
						normalizeIdentity(target.korName()) == normalizeIdentity(korName) &&
							target.distilleryId() == distilleryId &&
							normalizeNumericIdentity(target.abv()) == normalizeNumericIdentity(abvNormalized)
					}.mapNotNull { it.alcoholId() }
			} else {
				emptyList()
			}
		if (candidateIds.isNotEmpty()) {
			warnings +=
				issue(
					"DUPLICATE_CANDIDATE",
					null,
					"이미 등록된 위스키입니다 이름=$korName, 증류소ID=$distilleryId, 도수=$abvDisplay, 용량=$volumeDisplay, 후보ID=${candidateIds.joinToString(",")}",
				)
		}

		return AdminAlcoholExcelRowResult(
			rowNumber = parsed.rowNumber,
			korName = korName ?: parsed.korName.ifBlank { null },
			engName = engName ?: parsed.engName.ifBlank { null },
			abv = abvDisplay ?: abvRaw,
			type = typeName ?: typeRaw,
			korCategory = korCategory,
			engCategory = engCategory,
			categoryGroup = categoryGroupName ?: categoryGroupRaw,
			region = regionName,
			distillery = distilleryName,
			age = age ?: parsed.age.ifBlank { null },
			cask = cask ?: parsed.cask.ifBlank { null },
			description = description ?: parsed.description.ifBlank { null },
			volume = volumeDisplay ?: volumeRaw,
			tastingTags = parsed.tastingTagIds.ifBlank { null },
			regionId = regionId,
			distilleryId = distilleryId,
			tastingTagIds = tastingTagIds.takeIf { it.isNotEmpty() },
			candidateAlcoholIds = candidateIds.takeIf { it.isNotEmpty() },
			valid = errors.isEmpty(),
			errors = errors,
			warnings = warnings,
		)
	}

	private fun parseDecimal(
		raw: String,
		column: Column,
		errors: MutableList<AdminAlcoholExcelIssue>,
	): BigDecimal? {
		val cleaned = raw.trim().replace(",", "")
		if (!cleaned.matches(Regex("""^\d+(\.\d{1,2})?$"""))) {
			errors +=
				issue(
					"INVALID_NUMBER",
					column.header,
					"${column.header} 필드가 잘못 입력되었습니다. 숫자만 입력하고 소수 2자리까지 허용됩니다.",
				)
			return null
		}
		return BigDecimal(cleaned).setScale(2, RoundingMode.UNNECESSARY)
	}

	private fun parseLongId(
		raw: String,
		column: Column,
		errors: MutableList<AdminAlcoholExcelIssue>,
	): Long? {
		val cleaned = raw.trim()
		if (!cleaned.matches(Regex("""^\d+$"""))) {
			errors +=
				issue(
					"INVALID_ID",
					column.header,
					"${column.header} 필드가 잘못 입력되었습니다. 참조 시트의 ID 숫자를 입력하세요.",
				)
			return null
		}
		return cleaned.toLong()
	}

	private fun formatWithSuffix(
		value: BigDecimal,
		suffix: String,
	): String = value.stripTrailingZeros().toPlainString() + suffix

	private fun stripUnit(value: String?): String =
		value
			.orEmpty()
			.trim()
			.replace("%", "", ignoreCase = true)
			.replace("ml", "", ignoreCase = true)
			.trim()

	private fun normalizeNumericIdentity(value: String?): String {
		val cleaned = stripUnit(value)
		if (cleaned.isBlank()) return ""
		return runCatching {
			BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
		}.getOrElse { normalizeIdentity(cleaned) }
	}

	private fun normalizeNumericIdentity(value: BigDecimal?): String {
		if (value == null) return ""
		return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
	}

	private fun writeHeaderAndDescription(
		sheet: Sheet,
		styles: TemplateStyles,
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
		styles: TemplateStyles,
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
		styles: TemplateStyles,
	) {
		val lines =
			listOf(
				"BottleNote 알코올 일괄 등록 템플릿",
				"1페이지(이 시트)는 설명·예제·오류 코드입니다. 실제 입력은 마지막 시트 '알코올 데이터'에만 작성하세요.",
				"",
				"[시트 구성]",
				"1. 사용 안내: 설명, 예제, 오류 코드",
				"2. 지역: ID / 한글 이름 / 영문 이름",
				"3. 증류소: ID / 한글 이름 / 영문 이름",
				"4. 테이스팅 태그: ID / 한글 이름 / 영문 이름",
				"5. 카테고리: ID / 카테고리 그룹 / 한글 카테고리 / 영문 카테고리",
				"6. 알코올 데이터: 실제 입력 시트(마지막 고정)",
				"",
				"[입력 규칙]",
				"- 주류 종류, 카테고리 그룹만 한글 enum 값을 입력합니다.",
				"- 지역/증류소/테이스팅 태그/카테고리는 참조 시트의 ID를 입력합니다.",
				"- 도수와 용량은 숫자만 입력합니다. 소수 2자리까지 허용되며 서버가 % / ml를 붙입니다.",
				"- 설명(디스크립션)은 필수입니다.",
				"- 테이스팅 태그 ID는 여러 개일 때 | 로 구분합니다. 예: 1|3",
				"- 이미지는 이 템플릿에 포함되지 않습니다.",
				"- 수식 셀과 외부 링크는 허용되지 않습니다.",
				"",
				"[예제 1행]",
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
		categoryCount: Int,
	) {
		val helper: DataValidationHelper = dataSheet.dataValidationHelper

		fun namedRange(
			name: String,
			sheetName: String,
			columnLetter: String,
			rowCount: Int,
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
			explicitList: Array<String>?,
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
					columnIndex,
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

	private fun isCompletelyBlank(row: Row): Boolean =
		AlcoholExcelSchema.HEADERS.indices.all { index -> readRawCell(row.getCell(index)).isBlank() }

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

	private fun categoryStableId(item: CategoryItem): String {
		val group = item.categoryGroup()?.name.orEmpty()
		val kor = item.korCategory().orEmpty()
		val eng = item.engCategory().orEmpty()
		return listOf(group, kor, eng).joinToString("|")
	}

	private fun loadRegions(): List<AdminRegionItem> =
		regionRepository.findAllRegions(null, PageRequest.of(0, 10_000)).content

	private fun loadDistilleries(): List<AdminDistilleryItem> =
		distilleryRepository.findAllDistilleries(null, PageRequest.of(0, 10_000)).content

	private fun loadTastingTags(): List<TastingTagNodeItem> =
		tastingTagRepository.findAllTastingTags(null, PageRequest.of(0, 10_000)).content

	private fun normalizeIdentity(value: String?): String {
		if (value.isNullOrBlank()) return ""
		val nfkc = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
		return nfkc.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
	}

	private fun normalizedIdentityKey(parsed: ParsedRow): IdentityKey =
		IdentityKey(
			normalizeIdentity(parsed.korName),
			normalizeIdentity(parsed.distilleryId),
			normalizeNumericIdentity(parsed.abv),
			normalizeNumericIdentity(parsed.volume),
		)

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
		val tastingTagIds: String,
	)

	private data class IdentityKey(
		val name: String,
		val distilleryId: String,
		val abv: String,
		val volume: String,
	)

	private class TemplateStyles(
		workbook: Workbook,
	) {
		val title: CellStyle =
			workbook.createCellStyle().apply {
				setFont(
					workbook.createFont().apply {
						bold = true
						fontHeightInPoints = 14
						color = IndexedColors.DARK_BLUE.index
					},
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
					},
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
					},
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

		private fun CellStyle.setFont(font: Font) {
			this.setFont(font)
		}
	}
}
