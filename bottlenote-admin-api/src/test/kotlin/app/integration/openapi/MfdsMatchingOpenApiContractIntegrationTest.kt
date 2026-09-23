package app.integration.openapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("admin_integration")
@DisplayName("[integration] Admin MFDS 매칭 OpenAPI 계약")
class MfdsMatchingOpenApiContractIntegrationTest : OpenApiSpecTestSupport() {
	@Test
	@DisplayName("매칭 실행 문서에 위스키 10개와 증류소·지역 3개 제한을 명시한다")
	fun documentsCandidateLimits() {
		val operation = operationsOf(fetchSpec()).single {
			it.endpoint() == "POST /v1/mfds/declarations/{declarationId}/matching/run"
		}
		assertThat(operation.definition.path("description").asText())
			.contains("위스키는 상위 10개", "증류소·지역은 각각 상위 3개", "0.4 이상")
		assertThat(operation.successSchema().isMissingNode).isFalse()
		assertThat(operation.security().any { it.has("bearerAuth") }).isTrue()
	}

	@Test
	@DisplayName("매칭 점수 근거에 브랜드와 검토 여부 및 속성 비교를 공개한다")
	fun documentsStructuredEvidence() {
		val spec = fetchSpec()
		val properties = spec.at("/components/schemas/MfdsMatchScoreDetailItem/properties")
		assertThat(properties.has("brandScore")).isTrue()
		assertThat(properties.has("reviewRequired")).isTrue()
		assertThat(properties.has("comparisons")).isTrue()
		val operation = operationsOf(spec).single { it.endpoint() == "GET /v1/mfds/declarations/{declarationId}/matching/candidates" }
		assertThat(operation.definition.path("description").asText()).contains("고정 후보 컬럼은 참조하지 않습니다", "원래 척도")
	}
}
