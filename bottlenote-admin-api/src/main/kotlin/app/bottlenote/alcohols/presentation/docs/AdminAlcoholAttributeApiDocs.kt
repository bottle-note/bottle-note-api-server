package app.bottlenote.alcohols.presentation.docs

import app.bottlenote.alcohols.dto.response.*
import app.bottlenote.global.dto.response.AdminResultResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

object AdminAlcoholAttributeApiDocs {
	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "알코올")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "공통 속성 사전을 조회한다", description = "미연결 속성도 조회합니다. keyword는 표시명·키 검색, type은 분류, alcoholIds는 연결된 술 필터입니다. page/size로 페이지를 지정합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = AttributeListEnvelope::class))])
	annotation class FindAttributes

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "선택한 술의 속성 연결을 집계한다", description = "속성 ID별로 연결된 alcoholIds와 mappingIds, 술별 값이 포함된 mappings를 반환합니다. mappingIds는 연결 해제 대상이며 속성 삭제 ID가 아닙니다. alcoholIds가 없으면 빈 목록입니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(array = ArraySchema(schema = Schema(implementation = AdminAlcoholAttributeSummaryItem::class)))])
	annotation class SummarizeAttributes

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "속성이 연결된 술을 조회한다", description = "keyword는 술 이름, type·key는 속성 정의, value는 술별 연결 값 필터입니다. operator 기본값은 EQ이며 GTE는 숫자 속성의 key와 value가 필수입니다. missingKey는 해당 속성이 연결되지 않은 술을 찾습니다. 속성이 하나 이상 연결된 술을 이름순으로 반환합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = AttributedAlcoholListEnvelope::class))])
	annotation class FindAttributedAlcohols

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "공통 속성 한 건을 생성하거나 갱신한다", description = "key가 같으면 기존 ID와 생성 정보를 유지하고 전체 갱신합니다. 다른 키의 displayName과 충돌하면 409입니다. 변경 후 기존 매핑은 유지합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = AdminAlcoholAttributeItem::class))])
	annotation class CreateAttributes

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "공통 속성을 수정한다", description = "key는 변경하지 않습니다. 표시명·분류·자료형·단위·설명을 수정합니다. 연결된 속성의 자료형 변경은 409입니다. 이름 충돌은 409이며 기존 매핑은 유지합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = AdminResultResponse::class))])
	annotation class UpdateAttribute

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "공통 속성을 삭제한다", description = "속성 및 그 속성의 모든 술 연결을 함께 삭제합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = AdminResultResponse::class))])
	annotation class DeleteAttribute

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "공통 속성을 일괄 삭제한다", description = "선택한 속성과 관련 매핑을 함께 삭제하고 삭제한 속성을 반환합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(array = ArraySchema(schema = Schema(implementation = AdminAlcoholAttributeItem::class)))])
	annotation class DeleteAttributes

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "속성 키별 사용 현황을 조회한다", description = "미연결 속성도 포함하며 alcoholCount는 실제 연결된 술 수입니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(array = ArraySchema(schema = Schema(implementation = AlcoholAttributeKeyCountItem::class)))])
	annotation class FindAttributeKeys

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "키워드 속성의 사용 현황을 조회한다", description = "KEYWORD 값별 연결된 술 수를 중복 없이 집계합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(array = ArraySchema(schema = Schema(implementation = AlcoholAttributeKeywordCountItem::class)))])
	annotation class FindAttributeKeywords

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "속성 입력용 코드와 한글 설명을 조회한다", description = "분류·자료형·제공자·비교 조건의 코드 및 한글 설명을 반환합니다. 비교 조건은 검색에 사용하며 GTE는 숫자 자료형에만 허용됩니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = AdminAlcoholAttributeEnumsResponse::class))])
	annotation class FindAttributeEnums

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "공통 속성을 벌크 생성하거나 갱신한다", description = "items 최대 500개. key 중복은 기존 속성을 덮어쓰고 다른 키의 이름 중복은 409입니다. 요청 내부 동일 key는 400입니다. 오류 시 전체 롤백하며 created·updated·ids를 반환합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = AdminAlcoholAttributeBulkResponse::class))])
	annotation class UpsertAttributes

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "술과 속성을 벌크 연결한다", description = "items는 alcoholId·attributeId·value를 포함하며 최대 500개입니다. 기존 연결과 요청 내 중복은 건너뛰며 기존 값과 감사 정보도 유지합니다. 모든 value는 속성 자료형에 맞아야 하며 값 수정은 매핑 수정 API를 사용합니다. 없거나 삭제된 술·없는 속성은 404이며 요청 전체가 롤백됩니다. 조건으로 자동 연결하지 않습니다. createdIds는 신규 매핑 ID입니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = AdminAlcoholAttributeMappingBulkResponse::class))])
	annotation class CreateMappings

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "술과 속성의 연결을 조회한다", description = "alcoholId·attributeId로 필터링합니다. page/size 페이지와 술별 value, 생성·수정 시각 및 작업자 정보를 반환합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = MappingListEnvelope::class))])
	annotation class FindMappings

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "술과 속성의 연결을 해제한다", description = "매핑 ID로 연결 한 건을 삭제합니다. 공통 속성은 유지합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = AdminResultResponse::class))])
	annotation class DeleteMapping

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "술별 속성 값을 수정한다", description = "매핑 ID의 value만 수정합니다. 속성 자료형에 맞지 않는 값은 400이며 속성 정의와 다른 술의 값은 유지합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = AdminAlcoholAttributeMappingItem::class))])
	annotation class UpdateMapping

	@Schema(name = "AttributeListEnvelope")
	data class AttributeListEnvelope(
		val success: Boolean,
		val code: Int,
		val data: List<AdminAlcoholAttributeItem>,
		val errors: List<Any> = emptyList(),
		val meta: Map<String, Any?> = emptyMap()
	)

	@Schema(name = "MappingListEnvelope")
	data class MappingListEnvelope(
		val success: Boolean,
		val code: Int,
		val data: List<AdminAlcoholAttributeMappingItem>,
		val errors: List<Any> = emptyList(),
		val meta: Map<String, Any?> = emptyMap()
	)

	@Schema(name = "AttributedAlcoholListEnvelope")
	data class AttributedAlcoholListEnvelope(
		val success: Boolean,
		val code: Int,
		val data: List<AdminAttributedAlcoholItem>,
		val errors: List<Any> = emptyList(),
		val meta: Map<String, Any?> = emptyMap()
	)
}
