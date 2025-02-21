package app.external.push.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ConfigurationPropertiesScan
@ConfigurationProperties(prefix = "app.third-party")
public class FirebaseProperties {
	private String firebaseConfigurationFile;
	private String base64file;
}
