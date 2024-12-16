package app.bottlenote.config;

import app.bottlenote.user.config.OauthConfigProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfigProperties {

	@Bean
	public OauthConfigProperties oauthConfigProperties() {
		return OauthConfigProperties.builder()
			.guestCode("guest-code-for-test")
			.cookieExpireTime(100000)
			.refreshTokenHeaderPrefix("refresh-token")
			.build();
	}
}
