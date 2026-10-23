package app.bottlenote.alcohols.presentation.docs

import app.bottlenote.alcohols.dto.response.AdminScenarioResponse.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.*
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag

object AdminAlcoholScenarioApiDocs {
	@Target(AnnotationTarget.CLASS)
	@Retention(AnnotationRetention.RUNTIME)
	@Tag(name = "알코올")
	annotation class ApiTag

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "그룹의 시나리오 목록을 조회한다", description = "슬롯 순서로 최대 3개를 반환합니다. 비활성 시나리오도 슬롯을 점유합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(array = ArraySchema(schema = Schema(implementation = ScenarioItem::class)))])
	annotation class FindAll

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "빈 시나리오를 생성한다", description = "slot은 1~3이며 그룹에서 고유합니다. 중복·동시 슬롯 생성 충돌은 409입니다. 빈 DRAFT 버전 1을 함께 만들며 샘플 질문을 주입하지 않습니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = ScenarioDetail::class))])
	annotation class Create

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "시나리오와 버전 목록을 조회한다", description = "그룹 소유권을 검사하며 다른 그룹/시나리오/버전 참조는 404입니다. 감사 주체와 lockVersion을 포함합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = ScenarioDetail::class))])
	annotation class Detail

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "시나리오 이름과 활성 여부를 변경한다", description = "expectedLockVersion으로 시나리오 전체 변경을 비교하며 오래된 요청은 409입니다. enabled=false여도 게시 이력과 슬롯을 유지합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = ScenarioItem::class))])
	annotation class Update

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "초안만 있는 시나리오를 삭제한다", description = "expectedLockVersion을 비교합니다. 게시 이력이 있으면 409이며 비활성화를 사용합니다. 역참조 포인터를 정리한 뒤 V30의 부모→자식 CASCADE로 삭제합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = ScenarioDeleted::class))])
	annotation class Delete

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "빈 초안 또는 게시본 복제 초안을 생성한다", description = "expectedScenarioLockVersion을 비교하고 시나리오 revision을 증가시킵니다. sourceVersionId를 생략하면 빈 초안, 지정하면 같은 시나리오의 PUBLISHED 버전을 복제합니다. 모든 단계·질문·규칙에 새 ID를 발급하고 참조를 다시 연결합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = ScenarioVersionDetail::class))])
	annotation class Draft

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "버전의 전체 그래프를 조회한다", description = "단계 position과 질문 branchOrder는 배치 번호입니다. 실제 이동은 우선순위별 transition의 첫 일치 규칙 하나로만 결정합니다. 게시본은 불변이며 수정하려면 새 초안을 생성합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = ScenarioVersionDetail::class))])
	annotation class Version

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "초안의 전체 그래프를 원자적으로 저장한다", description = "미완성 초안은 허용하지만 타입·동일 그룹/버전 참조·순서 중복은 검증합니다. 입력되지 않은 하위 항목은 삭제하며 startQuestionId는 null로 정리할 수 있습니다. 기존 양수 ID는 유지하고 신규 음수 ID는 발급된 ID로 치환합니다. 동일 attributeId 재사용을 허용합니다. expectedLockVersion으로 하위 변경 전체를 비교하고 무변경 저장도 revision을 증가시킵니다. 오래된 저장은 409이며 부분 저장하지 않습니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = ScenarioVersionDetail::class))])
	annotation class Save

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "초안 버전을 삭제한다", description = "expectedScenarioLockVersion과 expectedLockVersion을 모두 비교합니다. PUBLISHED 버전은 삭제하지 않습니다. 시작 질문 및 활성 버전 역참조를 정리하고 자식만 CASCADE로 삭제합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = ScenarioDeleted::class))])
	annotation class DeleteDraft

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "검증한 초안을 게시한다", description = "시나리오와 버전의 expectedLockVersion을 모두 비교합니다. 시작 질문, 동일 버전 참조, 현재 그룹 속성, 입력/조건 JSON, 고유 순서, 전방 이동, 도달 가능성, 모든 경로의 명시적 END 및 출발 질문별 마지막 ALWAYS를 검증합니다. 실패는 400이며 질문·규칙 ID를 errors.message에 포함합니다. 게시와 activeVersionId 교체는 원자적이며 이전 게시본을 보존합니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = ScenarioDetail::class))])
	annotation class Publish

	@Target(AnnotationTarget.FUNCTION)
	@Retention(AnnotationRetention.RUNTIME)
	@Operation(summary = "저장한 시나리오 정의를 미리보기한다", description = "저장된 DRAFT/PUBLISHED를 엄격히 검증한 뒤 answers의 유효 경로와 후보를 계산합니다. CHOICE는 choiceId, NUMBER_RANGE는 양끝 포함 min/max, ANY는 조건 기여 없음입니다. GTE는 답변 하한, LTE는 상한을 비교합니다. changedQuestionId 뒤의 답변을 폐기하고 viewQuestionId 직전 조건으로 과거 카드 집계를 계산합니다. questionId별 조건을 유지하며 ATTRIBUTE/CATEGORY/TASTING_GROUP/ABV 모두 AND로 합성합니다. 모순은 후보 0개입니다. 기존 그룹 candidates 검색·페이징·집계를 재사용하며 후보 데이터는 현재 데이터입니다. 실행·답변은 저장하지 않습니다.")
	@ApiResponse(responseCode = "200", description = "처리 결과", content = [Content(schema = Schema(implementation = ScenarioPreview::class))])
	annotation class Preview
}
