package app.bottlenote.alcohols.presentation.docs

import app.bottlenote.alcohols.dto.response.AdminTastingTagGroupItem
import app.bottlenote.alcohols.dto.response.AdminTastingTagGroupListResponse
import app.bottlenote.global.dto.response.AdminResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

/** 맛 그룹 기준 정보 엔드포인트의 문서 설명. */
object AdminTastingTagGroupApiDocs {

	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "테이스팅 태그")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "맛 그룹 목록을 조회한다",
		description = """
			전체 맛 그룹과 각 그룹에 연결된 태그를 조회합니다.

			태그는 대표(CORE)를 먼저, 그다음 한글 이름 순으로 정렬합니다. 태그에는 역할·값 제공자의 영문 코드와 한글 표기, 그 태그가 붙은 위스키 수를 함께 내려줍니다.
			families는 그룹에 등장하는 계열의 중복 없는 목록이며, 그룹 정렬(계열·노출 순서·ID)에서 처음 나온 순서를 따릅니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "맛 그룹 목록",
				content = [Content(schema = Schema(implementation = AdminTastingTagGroupListResponse::class))]
			)
		]
	)
	annotation class ListGroups

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "맛 그룹 상세를 조회한다",
		description = "그룹 코드로 맛 그룹 한 건과 연결된 태그를 조회합니다. 목록의 그룹 한 건과 같은 모양입니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "맛 그룹 상세",
				content = [Content(schema = Schema(implementation = AdminTastingTagGroupItem::class))]
			),
			ApiResponse(responseCode = "404", description = "맛 그룹이 없습니다")
		]
	)
	annotation class GetGroup

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "맛 그룹을 등록한다",
		description = """
			새 맛 그룹을 등록합니다. code와 name은 각각 중복될 수 없습니다.

			code는 영문 대문자로 시작하고 대문자·숫자·밑줄만 64자까지 허용되며, 등록 후에는 바꿀 수 없습니다. family와 description, displayOrder는 생략할 수 있습니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "등록 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			),
			ApiResponse(responseCode = "409", description = "같은 코드 또는 이름의 맛 그룹이 이미 있습니다")
		]
	)
	annotation class CreateGroup

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "맛 그룹 정보를 수정한다",
		description = "이름, 계열, 설명, 노출 순서를 수정합니다. 코드는 바꾸지 않으며, displayOrder를 생략하면 기존 순서를 유지합니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "수정 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			),
			ApiResponse(responseCode = "404", description = "맛 그룹이 없습니다"),
			ApiResponse(responseCode = "409", description = "같은 이름의 맛 그룹이 이미 있습니다")
		]
	)
	annotation class UpdateGroup

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "맛 그룹을 삭제한다",
		description = "맛 그룹을 삭제합니다. 연결된 태그가 하나라도 있으면 삭제할 수 없습니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "삭제 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			),
			ApiResponse(responseCode = "404", description = "맛 그룹이 없습니다"),
			ApiResponse(responseCode = "409", description = "태그가 연결된 맛 그룹은 삭제할 수 없습니다")
		]
	)
	annotation class DeleteGroup

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "맛 그룹에 태그를 연결한다",
		description = """
			태그 ID 목록과 역할(CORE 또는 PERIPHERAL)을 받아 그룹에 연결합니다.

			존재하지 않는 태그 ID가 있으면 실패합니다. 이미 그룹에 있는 태그는 새로 추가하지 않고 역할만 바꿉니다.
			""",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "연결 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			),
			ApiResponse(responseCode = "404", description = "맛 그룹이 없거나 테이스팅 태그가 없습니다")
		]
	)
	annotation class AddTags

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "맛 그룹 태그의 역할을 변경한다",
		description = "그룹에 연결된 태그의 역할을 CORE 또는 PERIPHERAL로 바꿉니다. 그룹에 없는 태그는 바꿀 수 없습니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "역할 변경 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			),
			ApiResponse(responseCode = "404", description = "맛 그룹이 없거나 그룹에 해당 태그가 없습니다")
		]
	)
	annotation class ChangeTagRole

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(
		summary = "맛 그룹에서 태그를 제거한다",
		description = "그룹에 연결된 태그 하나를 제거합니다. 그룹에 없는 태그는 제거할 수 없습니다.",
		responses = [
			ApiResponse(
				responseCode = "200",
				description = "제거 처리 결과",
				content = [Content(schema = Schema(implementation = AdminResultResponse::class))]
			),
			ApiResponse(responseCode = "404", description = "맛 그룹이 없거나 그룹에 해당 태그가 없습니다")
		]
	)
	annotation class RemoveTag
}
