package app

import com.redis.testcontainers.RedisContainer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.MySQLContainer
import java.util.UUID

@Tag("admin_integration")
@DisplayName("[integration] Admin API 컨텍스트 로드 테스트")
class ApplicationContextStartupIntegrationTest : IntegrationTestSupport() {
	@Autowired
	private lateinit var mysqlContainer: MySQLContainer<Nothing>

	@Autowired
	private lateinit var redisContainer: RedisContainer

	@Autowired
	private lateinit var jdbcTemplate: JdbcTemplate

	@Test
	@DisplayName("VIEW가 존재해도 실제 테이블 초기화에 성공한다")
	fun cleansBaseTablesWhenViewExists() {
		val suffix = UUID.randomUUID().toString().replace("-", "")
		val tableName = "data_initializer_$suffix"
		val viewName = "${tableName}_view"
		try {
			jdbcTemplate.execute("CREATE TABLE $tableName (id BIGINT PRIMARY KEY)")
			jdbcTemplate.execute("CREATE VIEW $viewName AS SELECT id FROM $tableName")
			jdbcTemplate.update("INSERT INTO $tableName (id) VALUES (?)", 1L)

			dataInitializer.refreshCache()

			val tableCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $tableName", Long::class.java) ?: -1L
			assertThat(tableCount).isZero()
			assertThat(jdbcTemplate.queryForList("SELECT * FROM $viewName")).isEmpty()
		} finally {
			jdbcTemplate.execute("DROP VIEW IF EXISTS $viewName")
			jdbcTemplate.execute("DROP TABLE IF EXISTS $tableName")
			dataInitializer.refreshCache()
		}
	}

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
