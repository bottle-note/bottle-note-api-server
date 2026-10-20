package app.bottlenote.alcohols.presentation.docs

import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeGroupItem
import app.bottlenote.global.dto.response.AdminResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

object AdminAlcoholAttributeGroupApiDocs {
	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "알코올")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "속성 그룹 목록을 조회한다", description = "displayOrder와 ID 오름차순으로 모든 그룹을 반환합니다. 속성은 그룹 하나에 소속되거나 미분류 상태일 수 있습니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(array = ArraySchema(schema = Schema(implementation = AdminAlcoholAttributeGroupItem::class)))])
	annotation class FindGroups

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "속성 그룹을 생성한다", description = "이름은 공백 제거 후 1~100자이며 중복 이름은 409입니다. 설명은 최대 500자, displayOrder는 0 이상의 정수이며 생략 시 0입니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = AdminAlcoholAttributeGroupItem::class))])
	annotation class CreateGroup

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "속성 그룹을 수정한다", description = "이름·설명·정렬 순서를 갱신합니다. 소속 속성과 술 연결은 유지합니다. 없는 그룹은 404, 중복 이름은 409입니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = AdminAlcoholAttributeGroupItem::class))])
	annotation class UpdateGroup

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "속성 그룹을 삭제한다", description = "소속 속성이 남아 있으면 409입니다. 속성의 groupId를 null로 해제하거나 다른 그룹으로 옮긴 뒤 삭제합니다. 없는 그룹은 404입니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = AdminResultResponse::class))])
	annotation class DeleteGroup
}
