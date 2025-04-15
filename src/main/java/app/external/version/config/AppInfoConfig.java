package app.external.version.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "app.info")
public class AppInfoConfig {
	private String serverName;
	private String environment;
	private String gitBranch;
	private String gitCommit;
	private String gitBuildTime;
}
