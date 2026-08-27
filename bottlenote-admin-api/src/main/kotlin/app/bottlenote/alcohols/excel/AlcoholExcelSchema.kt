package app.bottlenote.alcohols.excel

/**
 * Admin 알코올 XLSX 템플릿의 고정 스키마.
 * 사용자에게 노출되는 헤더는 한글 필드명/설명만 사용한다.
 */
object AlcoholExcelSchema {
	const val DATA_SHEET_NAME = "알코올 데이터"
	const val REGION_SHEET_NAME = "지역"
	const val DISTILLERY_SHEET_NAME = "증류소"
	const val TASTING_TAG_SHEET_NAME = "테이스팅 태그"
	const val GUIDE_SHEET_NAME = "입력 안내"

	const val TEMPLATE_FILENAME = "alcohol-import-template.xlsx"
	const val XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

	const val HEADER_ROW_INDEX = 0
	const val DESCRIPTION_ROW_INDEX = 1
	const val DATA_START_ROW_INDEX = 2

	const val MAX_FILE_BYTES = 5L * 1024L * 1024L
	const val MAX_DATA_ROWS = 1000

	val HEADERS =
		listOf(
			"한글 이름",
			"영문 이름",
			"도수",
			"주류 종류",
			"한글 카테고리",
			"영문 카테고리",
			"카테고리 그룹",
			"지역",
			"증류소",
			"숙성 연도",
			"캐스크",
			"설명",
			"용량",
			"테이스팅 태그",
		)

	val DESCRIPTIONS =
		listOf(
			"제품의 한글 이름",
			"제품의 영문 이름",
			"도수 표기 (예: 40%)",
			"주류 종류 한글 표시값 (예: 위스키)",
			"하위 카테고리 한글명",
			"하위 카테고리 영문명",
			"카테고리 그룹 한글 표시값 (예: 싱글몰트 위스키)",
			"참조 시트의 지역 한글 이름",
			"참조 시트의 증류소 한글 이름",
			"숙성 연도 또는 표기값",
			"캐스크 타입",
			"제품 설명",
			"용량 표기 (예: 700ml)",
			"참조 시트의 테이스팅 태그 한글 이름. 여러 개는 | 로 구분",
		)

	enum class Column(
		val index: Int,
		val header: String,
	) {
		KOR_NAME(0, "한글 이름"),
		ENG_NAME(1, "영문 이름"),
		ABV(2, "도수"),
		TYPE(3, "주류 종류"),
		KOR_CATEGORY(4, "한글 카테고리"),
		ENG_CATEGORY(5, "영문 카테고리"),
		CATEGORY_GROUP(6, "카테고리 그룹"),
		REGION(7, "지역"),
		DISTILLERY(8, "증류소"),
		AGE(9, "숙성 연도"),
		CASK(10, "캐스크"),
		DESCRIPTION(11, "설명"),
		VOLUME(12, "용량"),
		TASTING_TAGS(13, "테이스팅 태그"),
	}
}
