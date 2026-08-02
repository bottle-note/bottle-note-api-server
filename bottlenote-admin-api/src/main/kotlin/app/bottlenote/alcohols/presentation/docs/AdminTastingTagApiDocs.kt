package app.bottlenote.alcohols.presentation.docs

import app.bottlenote.alcohols.dto.response.AdminTastingTagDetailResponse
import app.bottlenote.alcohols.dto.response.TastingTagNodeItem
import app.bottlenote.global.dto.response.AdminResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 테이스팅 태그 기준 정보 엔드포인트의 문서 설명. */
object AdminTastingTagApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "테이스팅 태그", description = "위스키 맛·향 테이스팅 태그를 등록·수정·삭제하고 위스키와의 연결을 관리한다")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "테이스팅 태그 목록을 조회한다",
		description = """
			검색어, 정렬 방향, 페이지 번호·크기로 테이스팅 태그 목록을 페이지 단위로 조회합니다.

			keyword를 비우면 전체 태그를 반환하며, 목록 항목의 parent/children 필드는 채우지 않습니다(상세 조회에서만 트리 구조를 반환합니다).
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "테이스팅 태그 목록",
				content = [
					Content(
						array = ArraySchema(schema = Schema(implementation = TastingTagNodeItem::class))
					)
				]
			)
		]
	)
	annotation class GetAllTastingTags

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "테이스팅 태그 상세 정보를 조회한다",
		description = """
			태그 ID로 단일 태그의 상세 정보를 조회합니다.

			부모 태그 체인과 자식 태그 트리를 마트료시카 구조(parent.parent... / children[].children[]...)로 함께 반환하며, 이 태그가 연결된 위스키 목록도 포함합니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "테이스팅 태그 상세 정보",
				content = [Content(schema = Schema(implementation = AdminTastingTagDetailResponse::class))]
			)
		]
	)
	annotation class GetTastingTagDetail

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "테이스팅 태그를 등록한다",
		description = """
			새 테이스팅 태그를 등록합니다.

			한글 이름이 이미 등록된 태그와 중복되면 실패합니다. parentId를 지정하면 하위 태그로 등록되며, 깊이는 최대 3단계까지만 허용됩니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "등록 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class CreateTastingTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "테이스팅 태그 정보를 수정한다",
		description = "기존 태그의 이름, 아이콘, 설명, 상위 태그를 수정합니다. 상위 태그를 바꾸면 다시 깊이 제한을 검사합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "수정 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class UpdateTastingTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "테이스팅 태그를 삭제한다",
		description = "태그를 삭제합니다. 하위 태그가 있거나 연결된 위스키가 하나라도 있으면 삭제할 수 없습니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "삭제 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class DeleteTastingTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "테이스팅 태그에 위스키를 연결한다",
		description = "위스키 ID 목록을 받아 해당 태그에 일괄로 연결합니다. 존재하지 않는 위스키 ID가 포함되면 실패합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "연결 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class AddAlcoholsToTastingTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "테이스팅 태그에서 위스키 연결을 해제한다",
		description = "위스키 ID 목록을 받아 해당 태그와의 연결을 일괄로 해제합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "연결 해제 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			)
		]
	)
	annotation class RemoveAlcoholsFromTastingTag
}
