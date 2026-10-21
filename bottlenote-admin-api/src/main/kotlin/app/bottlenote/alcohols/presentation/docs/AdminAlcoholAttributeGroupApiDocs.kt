package app.bottlenote.alcohols.presentation.docs

import app.bottlenote.alcohols.dto.response.AdminAlcoholAttributeGroupItem
import app.bottlenote.alcohols.dto.response.AdminAlcoholCandidateResponse
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

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "속성 그룹의 추천 후보를 조회한다", description = "그룹에 연결된 미삭제 술을 ID 오름차순으로 반환합니다. include는 AND, exclude는 하나라도 만족하면 술 전체를 제외합니다. 조건의 속성은 해당 그룹 소속이어야 하며 숫자 범위는 같은 연결 값에 적용됩니다. 도수 범위는 양끝 포함, 맛 그룹은 CORE 태그 기준입니다. page는 0부터, size는 기본 25·최대 100입니다. meta에 page·size·totalElements·totalPages·hasNext를 제공합니다. 전체 후보 수와 선택적인 nextQuestion 집계는 페이지와 무관합니다. 공통 속성 정의는 attributeDefinitions, 술별 값은 items.attributes로 반환합니다.")
	@ApiResponse(responseCode = "200", description = "후보 목록과 집계", content = [Content(schema = Schema(implementation = AdminAlcoholCandidateResponse::class))])
	annotation class FindCandidates
}
