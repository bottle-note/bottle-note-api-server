package app.global.security

import app.bottlenote.global.annotation.SecurityPolicy.AuthType.PUBLIC
import app.bottlenote.global.annotation.SecurityPolicy.AuthType.REQUIRED_AUTH
import app.bottlenote.global.security.policy.SecurityPolicyRegistry
import app.bottlenote.global.security.policy.SecurityPolicyRoute
import app.bottlenote.global.security.policy.SecurityPolicyRouteCollector
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping

@Configuration
class SecurityPolicyConfig {
	@Bean
	fun securityPolicyRegistry(
		@Qualifier("requestMappingHandlerMapping") handlerMapping: RequestMappingHandlerMapping
	): SecurityPolicyRegistry = SecurityPolicyRouteCollector.collect(
		handlerMapping,
		REQUIRED_AUTH,
		listOf(SecurityPolicyRoute.explicit(null, "/actuator/**", PUBLIC))
	)
}
