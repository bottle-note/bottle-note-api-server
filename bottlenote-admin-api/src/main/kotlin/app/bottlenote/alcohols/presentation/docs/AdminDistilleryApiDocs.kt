package app.bottlenote.alcohols.presentation.docs

import app.bottlenote.alcohols.dto.response.AdminDistilleryItem
import app.bottlenote.global.dto.response.AdminResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 증류소 기준 정보 엔드포인트의 문서 설명. */
object AdminDistilleryApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "증류소", description = "위스키 증류소 기준 정보를 등록·수정·삭제하고 정렬 순서를 관리한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "증류소 목록을 조회한다",
		description = """
			검색어, 정렬 방향, 페이지 번호·크기로 증류소 목록을 페이지 단위로 조회합니다.

			keyword를 비우면 전체 증류소를 반환하며, sortOrder를 지정하지 않으면 오름차순(ASC)이 기본값입니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "증류소 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = AdminDistilleryItem::class))
					)
				]
			)
		]
	)
	annotation class GetAllDistilleries

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "증류소 상세 정보를 조회한다",
		description = "증류소 ID로 단일 증류소의 상세 정보를 조회합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "증류소 상세 정보",
				content = [Content(schema = Schema(implementation = AdminDistilleryItem::class))]
			)
		]
	)
	annotation class GetDistilleryDetail

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "증류소를 등록한다",
		description = """
			새 증류소를 등록합니다.

			한글/영문 이름이 이미 등록된 증류소와 중복되면 실패합니다. 정렬 순서를 지정하지 않으면 맨 뒤에 배치됩니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "등록 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class CreateDistillery

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "증류소 정보를 수정한다",
		description = "기존 증류소의 이름, 이미지, 정렬 순서를 수정합니다. 이름을 변경하면 다른 증류소와의 중복 여부를 다시 검사합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "수정 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UpdateDistillery

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "증류소를 삭제한다",
		description = "증류소를 삭제합니다. 해당 증류소로 등록된 위스키가 하나라도 있으면 삭제할 수 없습니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "삭제 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class DeleteDistillery

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "증류소 정렬 순서를 변경한다",
		description = "단일 증류소의 정렬 순서 값을 변경합니다. 기존에 같은 순서 이상을 쓰던 증류소들은 뒤로 한 칸씩 밀립니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "정렬 순서 변경 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UpdateDistillerySortOrder

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "증류소 목록을 일괄 재정렬한다",
		description = """
			증류소 ID 목록을 원하는 순서대로 보내면 그 순서대로 정렬 순서를 다시 매깁니다.

			목록에 포함되지 않은 나머지 증류소는 기존 순서를 유지한 채 뒤로 이어 붙습니다. ID가 중복되면 실패합니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "재정렬 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class ReorderDistilleries
}
