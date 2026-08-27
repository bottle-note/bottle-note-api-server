package app.bottlenote.alcohols.presentation.docs

import app.bottlenote.alcohols.dto.response.AdminAlcoholDetailResponse
import app.bottlenote.alcohols.dto.response.AdminAlcoholExcelValidateResponse
import app.bottlenote.alcohols.dto.response.AdminAlcoholItem
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem
import app.bottlenote.alcohols.dto.response.CategoryPairItem
import app.bottlenote.global.dto.response.AdminResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

object AdminAlcoholsApiDocs {
	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "알코올", description = "Admin 알코올 조회·등록·수정·삭제·엑셀 검증 API")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "알코올 lookup 목록을 조회한다")
	@ApiResponse(
		responseCode = "200",
		description = "lookup 목록",
		content = [Content(schema = Schema(implementation = AlcoholLookupListEnvelope::class))],
	)
	annotation class GetAlcoholLookups

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "알코올 목록을 검색한다")
	@ApiResponse(
		responseCode = "200",
		description = "검색 결과",
		content = [Content(schema = Schema(implementation = AlcoholSearchEnvelope::class))],
	)
	annotation class SearchAlcohols

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "알코올 상세를 조회한다")
	@ApiResponse(
		responseCode = "200",
		description = "상세 결과",
		content = [Content(schema = Schema(implementation = AlcoholDetailEnvelope::class))],
	)
	annotation class GetAlcoholDetail

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "카테고리 참조 맵을 조회한다")
	@ApiResponse(
		responseCode = "200",
		description = "카테고리 참조",
		content = [Content(schema = Schema(implementation = CategoryReferenceEnvelope::class))],
	)
	annotation class GetCategoryReference

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "알코올을 생성한다")
	@ApiResponse(
		responseCode = "200",
		description = "생성 결과",
		content = [Content(schema = Schema(implementation = AdminResultEnvelope::class))],
	)
	annotation class CreateAlcohol

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "알코올을 수정한다")
	@ApiResponse(
		responseCode = "200",
		description = "수정 결과",
		content = [Content(schema = Schema(implementation = AdminResultEnvelope::class))],
	)
	annotation class UpdateAlcohol

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "알코올을 삭제한다")
	@ApiResponse(
		responseCode = "200",
		description = "삭제 결과",
		content = [Content(schema = Schema(implementation = AdminResultEnvelope::class))],
	)
	annotation class DeleteAlcohol

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "알코올 엑셀 템플릿을 다운로드한다",
		description = "1행 한글 필드명, 2행 한글 설명, 참조 시트와 입력 안내를 포함한 XLSX 템플릿을 반환한다.",
	)
	@ApiResponse(
		responseCode = "200",
		description = "XLSX 템플릿 바이너리",
		content = [
			Content(
				mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				schema = Schema(type = "string", format = "binary"),
			),
		],
	)
	annotation class DownloadAlcoholExcelTemplate

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "알코올 엑셀을 업로드해 검증한다",
		description = "DB에 저장하지 않고 파싱·검증 결과만 반환한다. 이미지 업로드는 포함하지 않는다.",
	)
	@ApiResponse(
		responseCode = "200",
		description = "검증 결과",
		content = [Content(schema = Schema(implementation = AlcoholExcelValidateEnvelope::class))],
	)
	annotation class ValidateAlcoholExcel

	@Schema(name = "AlcoholLookupListEnvelope")
	data class AlcoholLookupListEnvelope(
		val success: Boolean,
		val code: Int,
		val data: List<AlcoholLookupItem>,
		val errors: List<Any> = emptyList(),
		val meta: Map<String, Any?> = emptyMap(),
	)

	@Schema(name = "AlcoholSearchEnvelope")
	data class AlcoholSearchEnvelope(
		val success: Boolean,
		val code: Int,
		val data: List<AdminAlcoholItem>,
		val errors: List<Any> = emptyList(),
		val meta: Map<String, Any?> = emptyMap(),
	)

	@Schema(name = "AlcoholDetailEnvelope")
	data class AlcoholDetailEnvelope(
		val success: Boolean,
		val code: Int,
		val data: AdminAlcoholDetailResponse,
		val errors: List<Any> = emptyList(),
		val meta: Map<String, Any?> = emptyMap(),
	)

	@Schema(name = "CategoryReferenceEnvelope")
	data class CategoryReferenceEnvelope(
		val success: Boolean,
		val code: Int,
		val data: CategoryReferenceMap,
		val errors: List<Any> = emptyList(),
		val meta: Map<String, Any?> = emptyMap(),
	)

	@Schema(name = "AdminResultEnvelope")
	data class AdminResultEnvelope(
		val success: Boolean,
		val code: Int,
		val data: AdminResultResponse,
		val errors: List<Any> = emptyList(),
		val meta: Map<String, Any?> = emptyMap(),
	)

	@Schema(name = "AlcoholExcelValidateEnvelope")
	data class AlcoholExcelValidateEnvelope(
		val success: Boolean,
		val code: Int,
		val data: AdminAlcoholExcelValidateResponse,
		val errors: List<Any> = emptyList(),
		val meta: Map<String, Any?> = emptyMap(),
	)

	@Schema(name = "CategoryReferenceMap")
	data class CategoryReferenceMap(
		val SINGLE_MALT: List<CategoryPairItem>,
		val BLEND: List<CategoryPairItem>,
		val BLENDED_MALT: List<CategoryPairItem>,
		val BOURBON: List<CategoryPairItem>,
		val RYE: List<CategoryPairItem>,
		val OTHER: List<CategoryPairItem>,
	)
}
