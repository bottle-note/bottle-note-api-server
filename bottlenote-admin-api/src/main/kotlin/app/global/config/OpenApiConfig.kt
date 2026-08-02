package app.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

// 스펙 문서의 최상단 정보와 인증 방식을 정의한다. 개별 엔드포인트 설명은 각 도메인의 문서 어노테이션이 담당한다.
@Configuration
class OpenApiConfig {
	@Bean
	fun adminOpenApi(): OpenAPI = OpenAPI().info(adminApiInfo()).components(securityComponents())

	private fun adminApiInfo(): Info = Info()
		.title("보틀노트 Admin API")
		.version("v1")
		.description(
			"""
			보틀노트 어드민 콘솔이 사용하는 관리자 API입니다.

			모든 응답은 success, code, data, errors, meta를 갖는 공통 형식으로 감싸여 있고, 실제 결과값은 data 안에 담깁니다.

			인증이 필요한 엔드포인트는 로그인 후 발급받은 액세스 토큰을 Authorization 헤더에 "Bearer {토큰}" 형태로 담아 호출합니다.
			""".trimIndent()
		)

	private fun securityComponents(): Components = Components()
		.addSecuritySchemes(
			BEARER_AUTH,
			SecurityScheme()
				.type(SecurityScheme.Type.HTTP)
				.scheme("bearer")
				.bearerFormat("JWT")
				.description("로그인 응답으로 받은 액세스 토큰을 그대로 입력합니다.")
		)

	companion object {
		// components 하위 키는 OpenAPI 규칙상 영문 식별자여야 한다.
		const val BEARER_AUTH = "bearerAuth"
	}
}
