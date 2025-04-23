package app.bottlenote.global.redis.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Setter
@Getter
@Builder
@Configuration
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisConfigProperties {
	private String host;
	private int port;
	private String password;
	private long timeout;
	private Cluster cluster;

	/**
	 * Redis 설정 정보를 로그에 출력합니다.
	 * 애플리케이션 시작 시 호출하여 Redis 설정 상태를 확인할 수 있습니다.
	 */
	public void printConfigs() {
		log.info("Redis 설정 정보: host={}, port={}, password={}, timeout={}초, cluster.enabled={}",
				host, port, password != null && !password.isEmpty() ? "*****" : "설정 안됨", timeout,
				cluster != null && cluster.isEnabled());

		if (cluster != null && cluster.isEnabled() && cluster.getNodes() != null) {
			log.info("cluster.nodes: {}", cluster.getNodes());
		}

		log.info("Redis 연결 모드: {}",
				(cluster != null && cluster.isEnabled()) ? "클러스터 모드" : "단일 노드 모드");
	}

	@Getter
	@Setter
	public static class Cluster {
		private boolean enabled;
		private String nodes;
	}
}
