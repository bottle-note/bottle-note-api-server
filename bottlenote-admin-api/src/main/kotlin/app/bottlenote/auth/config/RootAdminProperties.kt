package app.bottlenote.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@ConfigurationProperties(prefix = "root.admin")
data class RootAdminProperties(
	val email: String,
	private val password: String
) {
	/**
	 * 주입받은 인코더를 사용하여 비밀번호를 암호화하여 반환합니다.
	 */
	fun getEncodedPassword(encoder: BCryptPasswordEncoder): String {
		return encoder.encode(password)
	}
}
