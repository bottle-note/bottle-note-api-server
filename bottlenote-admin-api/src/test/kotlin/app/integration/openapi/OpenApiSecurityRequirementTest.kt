package app.integration.openapi

import app.bottlenote.global.annotation.SecurityPolicy.AuthType
import app.bottlenote.global.security.policy.SecurityPolicyRouteCollector
import app.global.security.SecurityPolicyConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

/** 문서에 표시된 인증 요구가 실제 보안 정책과 어긋나지 않는지 검사한다. */
@Tag("admin_integration")
@DisplayName("[integration] Admin OpenAPI 인증 요구 표시")
class OpenApiSecurityRequirementTest : OpenApiSpecTestSupport() {

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private lateinit var handlerMapping: RequestMappingHandlerMapping

	@Test
	@DisplayName("문서의 토큰 요구는 실제 인증 정책과 일치한다")
	fun documentedTokenRequirementsMatchPolicies() {
		val policies = authTypesByEndpoint()
		val operations = operationsOf(fetchSpec())

		// 경로 매칭이 실패하면 아래 검사가 조용히 비어버리므로 먼저 고정한다
		val unmatched = operations.map { it.endpoint() }.filter { endpoint -> !policies.containsKey(endpoint) }
		assertThat(unmatched)
			.withFailMessage("실제 정책을 찾지 못한 엔드포인트가 있습니다:%n%s", joined(unmatched))
			.isEmpty()

		val violations =
			operations
				.filter { operation -> requiresBearer(operation) != (policies[operation.endpoint()] != AuthType.PUBLIC) }
				.map { operation -> "${operation.endpoint()} (정책 ${policies[operation.endpoint()]})" }

		assertThat(violations)
			.withFailMessage("문서의 토큰 요구가 실제 정책과 다릅니다:%n%s", joined(violations))
			.isEmpty()
	}

	@Test
	@DisplayName("토큰을 요구하는 엔드포인트가 문서에 실제로 존재한다")
	fun authenticatedOperationsExist() {
		val requiring = operationsOf(fetchSpec()).filter { operation -> requiresBearer(operation) }

		assertThat(requiring)
			.withFailMessage("토큰 요구가 하나도 실리지 않았습니다. 보안 스키마가 연결되지 않은 상태입니다")
			.isNotEmpty()
	}

	@Test
	@DisplayName("로그인·갱신·에이전트 로그인 3건만 인증 없이 호출 가능하다")
	fun onlyLoginRefreshAndAgentLoginArePublic() {
		val policies = authTypesByEndpoint()
		val publicEndpoints = policies.filterValues { it == AuthType.PUBLIC }.keys

		assertThat(publicEndpoints)
			.withFailMessage("PUBLIC 엔드포인트가 기대한 3건과 다릅니다:%n%s", joined(publicEndpoints.toList()))
			.isEqualTo(setOf("POST /auth/login", "POST /auth/refresh", "POST /auth/agent"))
	}

	/** 실제 보안 정책이 판정한 엔드포인트별 인증 방식. */
	private fun authTypesByEndpoint(): Map<String, AuthType> {
		val policies = mutableMapOf<String, AuthType>()
		handlerMapping.handlerMethods.forEach { (mapping, handlerMethod) ->
			val patterns = mapping.pathPatternsCondition ?: return@forEach
			val auth = SecurityPolicyRouteCollector.resolveAuthType(handlerMethod, SecurityPolicyConfig.FALLBACK_AUTH_TYPE)
			mapping.methodsCondition.methods.forEach { method ->
				patterns.patterns.forEach { pattern ->
					policies["${method.name} ${pattern.patternString}"] = auth
				}
			}
		}
		return policies
	}

	private fun requiresBearer(operation: SpecOperation): Boolean {
		for (item in operation.security()) {
			if (item.has(BEARER_AUTH)) {
				return true
			}
		}
		return false
	}

	companion object {
		private const val BEARER_AUTH = "bearerAuth"
	}
}
