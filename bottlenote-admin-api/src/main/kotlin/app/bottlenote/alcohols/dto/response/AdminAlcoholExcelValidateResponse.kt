package app.bottlenote.alcohols.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "AdminAlcoholExcelValidateResponse", description = "알코올 엑셀 업로드 검증 결과")
data class AdminAlcoholExcelValidateResponse(
	@Schema(description = "파싱된 데이터 행 수(빈 행 제외)")
	val totalRows: Int,
	@Schema(description = "오류 없는 행 수")
	val validRows: Int,
	@Schema(description = "하나 이상의 오류가 있는 행 수")
	val invalidRows: Int,
	@Schema(description = "하나 이상의 경고가 있는 행 수")
	val warningRows: Int,
	@Schema(description = "행별 검증 결과")
	val rows: List<AdminAlcoholExcelRowResult>
)

@Schema(name = "AdminAlcoholExcelRowResult", description = "알코올 엑셀 단일 행 검증 결과")
data class AdminAlcoholExcelRowResult(
	@Schema(description = "엑셀 행 번호(1-based, 헤더=1, 설명=2, 데이터 시작=3)")
	val rowNumber: Int,
	val korName: String?,
	val engName: String?,
	val abv: String?,
	val type: String?,
	val korCategory: String?,
	val engCategory: String?,
	val categoryGroup: String?,
	val region: String?,
	val distillery: String?,
	val age: String?,
	val cask: String?,
	val description: String?,
	val volume: String?,
	val tastingTags: String?,
	@Schema(description = "매칭된 지역 ID")
	val regionId: Long? = null,
	@Schema(description = "매칭된 증류소 ID")
	val distilleryId: Long? = null,
	@Schema(description = "매칭된 테이스팅 태그 ID 목록")
	val tastingTagIds: List<Long>? = null,
	@Schema(description = "강한 일치 후보 알코올 ID 목록")
	val candidateAlcoholIds: List<Long>? = null,
	val valid: Boolean,
	val errors: List<AdminAlcoholExcelIssue>,
	val warnings: List<AdminAlcoholExcelIssue>
)

@Schema(name = "AdminAlcoholExcelIssue", description = "행 단위 오류 또는 경고")
data class AdminAlcoholExcelIssue(
	@Schema(description = "안정적인 기계용 코드")
	val code: String,
	@Schema(description = "한글 필드명")
	val field: String?,
	@Schema(description = "사용자용 메시지")
	val message: String
)
