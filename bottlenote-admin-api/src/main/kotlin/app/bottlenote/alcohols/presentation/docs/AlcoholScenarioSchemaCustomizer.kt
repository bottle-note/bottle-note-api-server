package app.bottlenote.alcohols.presentation.docs

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.stereotype.Component

@Component
class AlcoholScenarioSchemaCustomizer : OpenApiCustomizer {
	override fun customise(openApi: OpenAPI) {
		val schemas = openApi.components?.schemas ?: return
		schemas["ScenarioDefinition"]?.apply {
			description = "정의 스키마 1의 전체 그래프. 양수 ID는 현재 버전의 기존 ID, 신규 항목은 요청 내 고유한 음수 임시 ID입니다. 참조에도 같은 ID를 사용하며 저장 응답은 발급된 양수 ID를 반환합니다. 기존 ID는 보존합니다. 배열 순서로 기본 이동을 만들지 않습니다. 최대 단계 50개·질문 100개·규칙 500개입니다."
			required = listOf("definitionSchemaVersion", "stages", "questions", "transitions")
			properties?.get("definitionSchemaVersion")?.description = "1만 지원합니다."
			properties?.get("stages")?.maxItems = 50
			properties?.get("questions")?.maxItems = 100
			properties?.get("transitions")?.maxItems = 500
			properties?.get("startQuestionId")?.description = "같은 버전의 질문 ID. 미완성 초안은 null을 허용합니다."
		}
		schemas["ScenarioInputConfig"]?.apply {
			description = "SINGLE_CHOICE는 allowAny(boolean)와 choices 배열만 사용합니다. NUMBER_RANGE는 allowAny와 min/max/step(number), 선택적 unit을 사용합니다. 게시에는 선택지 1~100개 또는 min<=max·step>0이 필수입니다. 미완성 초안은 빈 choices 또는 누락된 숫자 설정을 허용합니다. ANY는 미답변과 다르며 검색 조건을 만들지 않습니다. 미지원 필드와 숫자/boolean 문자열은 거절합니다."
			required = listOf("allowAny")
			additionalProperties = false
			properties?.get("choices")?.maxItems = 100
			listOf("min", "max", "step").forEach { properties?.get(it)?.types = setOf("number", "null") }
		}
		schemas["ScenarioChoice"]?.apply {
			description = "id는 문구와 독립된 안정적인 choiceId입니다. value는 ATTRIBUTE 자료형에 맞는 JSON scalar(STRING 문자열·INT 정수·DOUBLE 숫자·BOOLEAN boolean), CATEGORY enum 코드, TASTING_GROUP 코드, ABV 숫자(0~100)입니다. 상관없음은 선택지가 아니라 별도의 ANY 답변입니다."
			additionalProperties = false
			required = listOf("id", "label", "value")
			properties?.get("id")?.pattern = "[a-zA-Z0-9_-]{1,64}"
			properties?.put("value", Schema<Any>().oneOf(listOf(StringSchema(), NumberSchema(), BooleanSchema())))
		}
		schemas["ScenarioPredicate"]?.apply {
			description = "EQ는 SINGLE_CHOICE의 choiceId 문자열을 비교합니다. 문구나 value의 표시 문자열을 비교하지 않습니다. GTE는 NUMBER_RANGE 답변의 하한(min)>=value, LTE는 상한(max)<=value이며 경계를 포함합니다. 겹침 비교가 아닙니다. ANY는 ALWAYS에만 일치합니다. ALWAYS는 value 없이 출발 질문의 마지막 우선순위에 정확히 한 개 필요합니다."
			additionalProperties = false
			required = listOf("operator")
			properties?.put("value", Schema<Any>().oneOf(listOf(StringSchema(), NumberSchema(), Schema<Any>().types(setOf("null")))))
		}
		schemas["AdminScenarioPreviewRequest"]?.description = "무상태 미리보기입니다. answers는 실행 순서대로 제출합니다. changedQuestionId를 지정하면 그 답변 뒤의 모든 답변을 폐기하고 경로·조건을 재계산합니다. 미지정 시 중복/경로 밖/건너뛴 답변은 오류입니다. viewQuestionId는 유효한 과거 또는 현재 카드이며 그 카드 직전의 조건만 후보/집계에 적용합니다. 모든 출처의 조건은 AND입니다."
		schemas["ScenarioAnswer"]?.apply {
			description = "CHOICE는 questionId/kind/choiceId만, NUMBER_RANGE는 questionId/kind/min/max만, ANY는 questionId/kind만 사용합니다. 범위는 양끝 포함이며 설정 안에서 (값-min)이 step의 정수배여야 합니다."
			additionalProperties = false
			listOf("min", "max").forEach { properties?.get(it)?.types = setOf("number", "null") }
		}
		schemas["Draft"]?.properties?.get("sourceVersionId")?.description = "생략하면 빈 초안, 지정하면 같은 시나리오의 PUBLISHED 버전만 복제합니다. 모든 하위 ID와 참조를 새로 발급합니다."
	}
}
