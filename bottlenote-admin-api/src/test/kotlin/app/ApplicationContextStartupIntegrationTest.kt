package app

import com.redis.testcontainers.RedisContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.testcontainers.containers.MySQLContainer

@Tag("integration")
@DisplayName("[integration] Admin API 컨텍스트 로드 테스트")
class ApplicationContextStartupIntegrationTest : IntegrationTestSupport() {

	@Autowired
	private lateinit var mysqlContainer: MySQLContainer<Nothing>

	@Autowired
	private lateinit var redisContainer: RedisContainer

	@Test
	@DisplayName("컨텍스트 로드 및 컨테이너 상태 확인")
	fun contextLoads() {
		log.info("=== MySQL Container ===")
		log.info("Image: {}", mysqlContainer.dockerImageName)
		log.info("Container ID: {}", mysqlContainer.containerId)
		log.info("Host: {}", mysqlContainer.host)
		log.info("Port: {}", mysqlContainer.firstMappedPort)
		log.info("Database: {}", mysqlContainer.databaseName)
		log.info("Running: {}", mysqlContainer.isRunning)

		log.info("=== Redis Container ===")
		log.info("Image: {}", redisContainer.dockerImageName)
		log.info("Container ID: {}", redisContainer.containerId)
		log.info("Host: {}", redisContainer.host)
		log.info("Port: {}", redisContainer.firstMappedPort)
		log.info("Running: {}", redisContainer.isRunning)

		assertThat(mysqlContainer.isRunning).isTrue()
		assertThat(redisContainer.isRunning).isTrue()
		log.info("Context loaded successfully - All containers running")
	}
}
