package app.bottlenote.alcohols.presentation.docs

import app.bottlenote.alcohols.dto.response.AdminAlcoholDetailResponse
import app.bottlenote.alcohols.dto.response.AdminAlcoholItem
import app.bottlenote.alcohols.dto.response.AlcoholLookupItem
import app.bottlenote.alcohols.dto.response.CategoryPairItem
import app.bottlenote.global.dto.response.AdminResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 위스키 기본 정보 엔드포인트의 문서 설명. */
object AdminAlcoholsApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "위스키", description = "위스키 기본 정보를 등록·수정·삭제하고 목록·상세·룩업·카테고리 기준 정보를 조회한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "위스키 룩업 목록을 조회한다",
		description = """
			검색어, 카테고리, 지역, 증류소, 커서로 위스키 룩업 목록을 커서 방식으로 조회합니다.

			다른 화면에서 위스키를 빠르게 선택할 때 쓰는 경량 목록이며, Redis 스냅샷을 우선 사용하고 비어 있으면 DB로 폴백합니다.
			다음 페이지 정보는 meta.pagination에, 요청 파라미터는 meta.searchParameters에 담깁니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "위스키 룩업 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = AlcoholLookupItem::class))
					)
				]
			)
		]
	)
	annotation class GetAlcoholLookups

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "위스키 목록을 검색한다",
		description = "검색어, 카테고리, 지역, 정렬 기준·방향, 페이지 번호·크기, 삭제 데이터 포함 여부로 위스키 목록을 페이지 단위로 조회합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "위스키 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = AdminAlcoholItem::class))
					)
				]
			)
		]
	)
	annotation class SearchAlcohols

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "위스키 상세 정보를 조회한다",
		description = "위스키 ID로 단일 위스키의 상세 정보를 조회합니다. 지역·증류소·테이스팅 태그 정보와 평점·리뷰·픽 집계를 함께 반환합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "위스키 상세 정보",
				content = [Content(schema = Schema(implementation = AdminAlcoholDetailResponse::class))]
			)
		]
	)
	annotation class GetAlcoholDetail

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "카테고리 기준 정보를 조회한다",
		description = """
			등록·수정 폼에서 카테고리를 고를 때 쓰는 기준 정보입니다.

			카테고리 그룹(AlcoholCategoryGroup) 이름을 키로, 그 그룹에 속한 한글/영문 카테고리 쌍(CategoryPairItem) 배열을 값으로 갖는 객체를 반환합니다.
			데이터가 없는 그룹도 빈 배열로 포함되며, 그룹 순서는 enum 선언 순서로 고정됩니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "카테고리 그룹별 카테고리 쌍 목록",
				content = [
					Content(schema = Schema(implementation = AlcoholCategoryReferenceSchema::class))
				]
			)
		]
	)
	annotation class GetCategoryReference

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "위스키를 등록한다",
		description = "새 위스키를 등록합니다. 이름, 도수, 타입, 카테고리, 지역, 증류소 등 기본 정보를 입력하며, tastingTagIds로 테이스팅 태그를 함께 연결할 수 있습니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "등록 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class CreateAlcohol

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "위스키 정보를 수정한다",
		description = "기존 위스키의 기본 정보를 수정합니다. 이미 삭제된 위스키는 수정할 수 없습니다. tastingTagIds를 보내면 기존 연결을 모두 교체합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "수정 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UpdateAlcohol

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "위스키를 삭제한다",
		description = "위스키를 삭제합니다. 이미 삭제된 위스키이거나 리뷰·평점이 하나라도 있으면 삭제할 수 없습니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "삭제 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class DeleteAlcohol
}

@Schema(name = "AlcoholCategoryReference", description = "카테고리 그룹별 한글·영문 카테고리 쌍 목록")
internal data class AlcoholCategoryReferenceSchema(
	val SINGLE_MALT: List<CategoryPairItem>,
	val BLEND: List<CategoryPairItem>,
	val BLENDED_MALT: List<CategoryPairItem>,
	val BOURBON: List<CategoryPairItem>,
	val RYE: List<CategoryPairItem>,
	val OTHER: List<CategoryPairItem>
)
