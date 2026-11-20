package app.global.admin.security

import org.springframework.boot.context.properties.ConfigurationProperties

// admin 앱의 app.cors 값을 product의 app.cors와 섞지 않으려고 하위 키로 분리했다.
@ConfigurationProperties(prefix = "app.cors.admin")
data class AdminCorsProperties(
	val allowedOrigins: List<String>,
	val docsAllowedOrigins: List<String>
)
