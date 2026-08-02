package app.bottlenote.alcohols.presentation.docs

import app.bottlenote.alcohols.dto.response.AdminRegionDetailResponse
import app.bottlenote.alcohols.dto.response.AdminRegionItem
import app.bottlenote.global.dto.response.AdminResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 지역 기준 정보 엔드포인트의 문서 설명. */
object AdminRegionApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "지역", description = "위스키 생산 지역 기준 정보를 등록·수정·삭제하고 정렬 순서를 관리한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "지역 목록을 조회한다",
		description = """
			검색어, 정렬 방향, 페이지 번호·크기로 지역 목록을 페이지 단위로 조회합니다.

			keyword를 비우면 전체 지역을 반환하며, 부모/자식 지역이 모두 같은 목록에 평면적으로 포함됩니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "지역 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = AdminRegionItem::class))
					)
				]
			)
		]
	)
	annotation class GetAllRegions

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "지역 상세 정보를 조회한다",
		description = "지역 ID로 단일 지역의 상세 정보를 조회합니다. 부모 지역 정보, 자식 지역 존재 여부, 연결된 위스키 수를 함께 반환합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "지역 상세 정보",
				content = [Content(schema = Schema(implementation = AdminRegionDetailResponse::class))]
			)
		]
	)
	annotation class GetRegionDetail

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "지역을 등록한다",
		description = """
			새 지역을 등록합니다.

			한글/영문 이름이 중복되면 실패합니다. parentId를 지정하면 해당 지역의 하위 지역으로 등록되며, 깊이는 최대 2단계(부모-자식)까지만 허용됩니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "등록 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class CreateRegion

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "지역 정보를 수정한다",
		description = "기존 지역의 이름, 대륙, 설명, 이미지, 상위 지역, 정렬 순서를 수정합니다. 자기 자신을 부모로 지정하거나 순환 참조가 발생하면 실패합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "수정 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UpdateRegion

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "지역을 삭제한다",
		description = "지역을 삭제합니다. 하위 지역이 있거나 해당 지역으로 등록된 위스키가 있으면 삭제할 수 없습니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "삭제 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class DeleteRegion

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "지역 정렬 순서를 변경한다",
		description = "단일 지역의 정렬 순서 값을 변경합니다. 기존에 같은 순서 이상을 쓰던 지역들은 뒤로 한 칸씩 밀립니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "정렬 순서 변경 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UpdateRegionSortOrder

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "최상위 지역 목록을 일괄 재정렬한다",
		description = """
			지역 ID 목록을 원하는 순서대로 보내면 그 순서대로 정렬 순서를 다시 매깁니다.

			목록에 포함되지 않은 나머지 지역은 기존 순서를 유지한 채 뒤로 이어 붙습니다. ID가 중복되면 실패합니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "재정렬 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class ReorderRegions

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "특정 지역의 하위 지역 목록을 일괄 재정렬한다",
		description = "부모 지역 ID로 범위를 한정해, 그 하위 지역들만 요청한 순서대로 정렬 순서를 다시 매깁니다. 부모 지역이 없으면 실패합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "재정렬 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class ReorderRegionChildren
}
