package app.global.config

import app.bottlenote.global.annotation.SecurityPolicy.AuthType
import app.bottlenote.global.security.policy.SecurityPolicyRouteCollector
import app.global.security.SecurityPolicyConfig
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.security.SecurityRequirement
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

// 엔드포인트가 액세스 토큰을 요구하는지 문서에 표시한다.
// 판정 근거는 실제 인증과 같은 @SecurityPolicy다. 문서에 따로 적지 않으므로 정책을 바꾸면 문서도 함께 바뀐다.
@Component
class SecurityRequirementCustomizer : OperationCustomizer {

	override fun customize(operation: Operation, handlerMethod: HandlerMethod): Operation {
		val auth = SecurityPolicyRouteCollector.resolveAuthType(handlerMethod, SecurityPolicyConfig.FALLBACK_AUTH_TYPE)

		if (auth == AuthType.PUBLIC) {
			return operation
		}

		operation.addSecurityItem(SecurityRequirement().addList(OpenApiConfig.BEARER_AUTH))
		if (auth == AuthType.OPTIONAL_AUTH) {
			// 토큰 없이 호출해도 동작한다는 사실을 빈 요구사항으로 함께 알린다
			operation.addSecurityItem(SecurityRequirement())
		}
		return operation
	}
}
