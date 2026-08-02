package app.bottlenote.alcohols.presentation.docs

import app.bottlenote.alcohols.dto.response.AdminCurationDetailResponse
import app.bottlenote.alcohols.dto.response.AdminCurationListResponse
import app.bottlenote.global.dto.response.AdminResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 큐레이션 엔드포인트의 문서 설명. */
object AdminCurationApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "큐레이션", description = "위스키를 묶어 노출하는 큐레이션을 등록·수정·삭제하고 정렬 순서와 포함 위스키를 관리한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "큐레이션 목록을 조회한다",
		description = """
			검색어, 활성화 상태, 페이지 번호·크기로 큐레이션 목록을 페이지 단위로 조회합니다.

			isActive를 비우면 활성/비활성 여부와 관계없이 전체 큐레이션을 반환합니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "큐레이션 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = AdminCurationListResponse::class))
					)
				]
			)
		]
	)
	annotation class GetAllCurations

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "큐레이션 상세 정보를 조회한다",
		description = "큐레이션 ID로 단일 큐레이션의 상세 정보를 조회합니다. 포함된 위스키 목록을 함께 반환합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "큐레이션 상세 정보",
				content = [Content(schema = Schema(implementation = AdminCurationDetailResponse::class))]
			)
		]
	)
	annotation class GetCurationDetail

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "큐레이션을 등록한다",
		description = "새 큐레이션을 등록합니다. 이름이 이미 등록된 큐레이션과 중복되면 실패합니다. alcoholIds로 포함할 위스키를 함께 지정할 수 있습니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "등록 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class CreateCuration

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "큐레이션 정보를 수정한다",
		description = "기존 큐레이션의 이름, 설명, 커버 이미지, 노출 순서, 활성화 상태, 포함 위스키 목록을 한 번에 수정합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "수정 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UpdateCuration

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "큐레이션을 삭제한다",
		description = "큐레이션을 삭제합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "삭제 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class DeleteCuration

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "큐레이션 활성화 상태를 변경한다",
		description = "큐레이션의 활성/비활성 상태만 변경합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "활성화 상태 변경 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UpdateCurationStatus

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "큐레이션 노출 순서를 변경한다",
		description = "단일 큐레이션의 노출 순서 값을 변경합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "노출 순서 변경 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UpdateCurationDisplayOrder

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "큐레이션 목록을 일괄 재정렬한다",
		description = """
			큐레이션 ID 목록을 원하는 순서대로 보내면 그 순서대로 노출 순서를 다시 매깁니다.

			목록에 포함되지 않은 나머지 큐레이션은 기존 순서를 유지한 채 뒤로 이어 붙습니다. ID가 중복되면 실패합니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "재정렬 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class ReorderCurations

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "큐레이션에 위스키를 추가한다",
		description = "위스키 ID 목록을 받아 해당 큐레이션에 일괄로 추가합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "추가 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class AddAlcoholsToCuration

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "큐레이션에서 위스키를 제거한다",
		description = "큐레이션에 포함된 위스키 하나를 제거합니다. 포함되지 않은 위스키 ID를 보내면 실패합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "제거 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class RemoveAlcoholFromCuration
}
