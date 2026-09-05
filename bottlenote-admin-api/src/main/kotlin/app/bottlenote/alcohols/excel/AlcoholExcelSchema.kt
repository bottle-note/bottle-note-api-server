package app.bottlenote.alcohols.excel

/**
 * Admin 알코올 XLSX 템플릿의 고정 입력 열 스키마.
 * 사용자에게 노출되는 헤더는 한글 필드명/설명만 사용한다.
 *
 * 템플릿 기본 시트 구성:
 * 1. 사용 안내 (설명/예제/오류 코드 첫 페이지)
 * 2. 지역
 * 3. 증류소
 * 4. 테이스팅 태그
 * 5. 카테고리
 * 6. 알코올 데이터 (실제 입력 시트)
 *
 * 검증할 때는 알코올 데이터 시트, 첫 행의 정확한 13개 헤더와 두 번째 설명 행이 필수다.
 * 안내·참조 시트의 순서와 추가 메모 시트는 허용한다.
 *
 * 매핑 규칙:
 * - 주류 종류, 카테고리 그룹: 한글 표시값 또는 enum 이름
 * - 지역/증류소/테이스팅 태그: 존재하는 ID, 카테고리: 그룹|한글|영문 안정 키
 * - 도수: % 표기 또는 숫자, 용량: ml·cl·L 표기 또는 숫자
 */
object AlcoholExcelSchema {
	const val GUIDE_SHEET_NAME = "사용 안내"
	const val REGION_SHEET_NAME = "지역"
	const val DISTILLERY_SHEET_NAME = "증류소"
	const val TASTING_TAG_SHEET_NAME = "테이스팅 태그"
	const val CATEGORY_SHEET_NAME = "카테고리"
	const val DATA_SHEET_NAME = "알코올 데이터"

	const val TEMPLATE_FILENAME = "alcohol-import-template.xlsx"
	const val XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

	const val HEADER_ROW_INDEX = 0
	const val DESCRIPTION_ROW_INDEX = 1
	const val DATA_START_ROW_INDEX = 2

	const val MAX_FILE_BYTES = 5L * 1024L * 1024L
	const val MAX_DATA_ROWS = 1000

	val SHEET_ORDER =
		listOf(
			GUIDE_SHEET_NAME,
			REGION_SHEET_NAME,
			DISTILLERY_SHEET_NAME,
			TASTING_TAG_SHEET_NAME,
			CATEGORY_SHEET_NAME,
			DATA_SHEET_NAME
		)

	val HEADERS =
		listOf(
			"한글 이름",
			"영문 이름",
			"도수",
			"주류 종류",
			"카테고리 ID",
			"카테고리 그룹",
			"지역 ID",
			"증류소 ID",
			"숙성 연도",
			"캐스크",
			"설명",
			"용량",
			"테이스팅 태그 ID"
		)

	val DESCRIPTIONS =
		listOf(
			"제품의 한글 이름",
			"제품의 영문 이름",
			"숫자 또는 % 표기 (예: 40, 40%, 40.50%). 퍼센트 서식 셀도 허용합니다",
			"주류 종류 한글 표시값 또는 enum 이름 (예: 위스키, WHISKY)",
			"카테고리 시트의 ID(그룹|한글|영문 안정 키)를 입력합니다",
			"선택: 카테고리 ID의 그룹을 자동 사용합니다. 입력 시 한글 표시값 또는 enum 이름을 허용합니다",
			"지역 시트의 ID를 입력합니다",
			"증류소 시트의 ID를 입력합니다",
			"선택: 숙성 연도 또는 표기값",
			"선택: 캐스크 타입",
			"선택: 제품 설명",
			"숫자 또는 단위 표기 (예: 700ml, 70cl, 0.7L)",
			"테이스팅 태그 시트의 ID. 여러 개는 | 로 구분 (예: 1|3)"
		)

	/** 사용 안내 시트 예제 1행 (실제 입력 시트에는 넣지 않음). */
	val EXAMPLE_ROW =
		listOf(
			"글렌피딕 12년",
			"Glenfiddich 12 Year Old",
			"40.00",
			"위스키",
			"SINGLE_MALT|싱글 몰트|Single Malt",
			"싱글몰트 위스키",
			"10",
			"25",
			"12",
			"American Oak",
			"스페이사이드 대표 싱글몰트 위스키",
			"700.00",
			"1|2"
		)

	data class ErrorCatalogItem(
		val code: String,
		val messageTemplate: String
	)

	/** 사용 안내 시트에 노출하는 오류/경고 코드 정의. */
	val ERROR_CATALOG =
		listOf(
			ErrorCatalogItem("REQUIRED_FIELD", "{{필드}} 필드가 누락되었거나 비어 있습니다."),
			ErrorCatalogItem("INVALID_NUMBER", "{{필드}} 필드의 숫자·단위 또는 범위가 올바르지 않습니다."),
			ErrorCatalogItem("INVALID_ENUM_VALUE", "{{필드}} 필드가 잘못 입력되었습니다. 한글 표시값 또는 enum 이름을 입력하세요."),
			ErrorCatalogItem("INVALID_ID", "{{필드}} 필드는 Long 범위의 참조 ID여야 합니다."),
			ErrorCatalogItem("REGION_NOT_FOUND", "지역 ID를 찾을 수 없습니다: {{정보}}"),
			ErrorCatalogItem("DISTILLERY_NOT_FOUND", "증류소 ID를 찾을 수 없습니다: {{정보}}"),
			ErrorCatalogItem("UNKNOWN_CATEGORY", "기존 참조에 없는 카테고리 조합을 보존합니다."),
			ErrorCatalogItem(
				"CATEGORY_GROUP_MISMATCH",
				"카테고리 ID와 카테고리 그룹이 일치하지 않습니다: {{정보}}"
			),
			ErrorCatalogItem("TASTING_TAG_NOT_FOUND", "테이스팅 태그 ID를 찾을 수 없습니다: {{정보}}"),
			ErrorCatalogItem("DUPLICATE_TASTING_TAG", "중복된 테이스팅 태그 ID를 제거했습니다: {{정보}}"),
			ErrorCatalogItem("DUPLICATE_IN_FILE", "파일 내부 중복 후보입니다. 저장은 가능하지만 확인이 필요합니다: {{정보}}"),
			ErrorCatalogItem(
				"DUPLICATE_CANDIDATE",
				"이미 등록된 알코올 후보입니다 {{정보}}"
			),
			ErrorCatalogItem("NON_SCALAR_VALUE", "범위·배치·세트 표현을 원문으로 보존합니다."),
			ErrorCatalogItem("TYPE_GROUP_MISMATCH", "주류 타입과 카테고리 그룹의 의미를 확인해 주세요."),
			ErrorCatalogItem("EXCEL_INVALID_FILE_TYPE", "OOXML .xlsx 파일만 업로드할 수 있습니다."),
			ErrorCatalogItem("EXCEL_FILE_TOO_LARGE", "엑셀 파일 크기는 5MiB를 초과할 수 없습니다."),
			ErrorCatalogItem("EXCEL_SHEET_NOT_FOUND", "필수 시트가 없거나 시트명이 올바르지 않습니다."),
			ErrorCatalogItem("EXCEL_HEADER_MISMATCH", "엑셀 헤더(1행)가 고정 템플릿과 일치하지 않습니다."),
			ErrorCatalogItem("EXCEL_DESCRIPTION_MISMATCH", "엑셀 설명 행(2행)을 확인할 수 없습니다. 템플릿을 다시 내려받아 데이터는 3행부터 입력해 주세요."),
			ErrorCatalogItem("EXCEL_DUPLICATE_HEADER", "엑셀 헤더에 중복된 필드명이 있습니다."),
			ErrorCatalogItem("EXCEL_FORMULA_NOT_ALLOWED", "수식 셀은 허용되지 않습니다."),
			ErrorCatalogItem("EXCEL_EXTERNAL_LINK_NOT_ALLOWED", "외부 링크는 허용되지 않습니다."),
			ErrorCatalogItem("EXCEL_ROW_LIMIT_EXCEEDED", "데이터 행은 최대 1,000행까지 검증할 수 있습니다.")
		)

	enum class Column(
		val index: Int,
		val header: String
	) {
		KOR_NAME(0, "한글 이름"),
		ENG_NAME(1, "영문 이름"),
		ABV(2, "도수"),
		TYPE(3, "주류 종류"),
		CATEGORY_ID(4, "카테고리 ID"),
		CATEGORY_GROUP(5, "카테고리 그룹"),
		REGION_ID(6, "지역 ID"),
		DISTILLERY_ID(7, "증류소 ID"),
		AGE(8, "숙성 연도"),
		CASK(9, "캐스크"),
		DESCRIPTION(10, "설명"),
		VOLUME(11, "용량"),
		TASTING_TAG_IDS(12, "테이스팅 태그 ID")
	}
}
