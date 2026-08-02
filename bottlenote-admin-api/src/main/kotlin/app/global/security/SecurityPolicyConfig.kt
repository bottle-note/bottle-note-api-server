package app.global.security

import app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC
import app.bottlenote.global.annotation.SecurityPolicy.AuthType.REQUIRED_AUTH
import app.bottlenote.global.security.policy.SecurityPolicyRegistry
import app.bottlenote.global.security.policy.SecurityPolicyRoute
import app.bottlenote.global.security.policy.SecurityPolicyRouteCollector
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@Configuration
class SecurityPolicyConfig {
	@Bean
	fun securityPolicyRegistry(
		@Qualifier("requestMappingHandlerMapping") handlerMapping: RequestMappingHandlerMapping,
		@Value("\${springdoc.api-docs.path}") specPath: String
	): SecurityPolicyRegistry = SecurityPolicyRouteCollector.collect(
		handlerMapping,
		REQUIRED_AUTH,
		listOf(
			SecurityPolicyRoute.explicit(null, "/actuator/**", PUBLIC),
			SecurityPolicyRoute.explicit(null, "/error", PUBLIC),
			// API 스펙은 클라이언트 개발자가 인증 없이 받아갈 수 있어야 한다
			SecurityPolicyRoute.explicit(null, specPath, PUBLIC)
		)
	)
}
