package app.helper

import com.redis.testcontainers.RedisContainer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestContainersConfig {

	@Bean
	@ServiceConnection
	fun mysqlContainer(): MySQLContainer<Nothing> =
		MySQLContainer<Nothing>("mysql:8.0.32").apply {
			withReuse(true)
			withDatabaseName("bottlenote")
			withUsername("root")
			withPassword("root")
			withInitScripts(
				"storage/mysql/init/00-init-config-table.sql",
				"storage/mysql/init/01-init-core-table.sql"
			)
		}

	@Bean
	@ServiceConnection
	fun redisContainer(): RedisContainer =
		RedisContainer(DockerImageName.parse("redis:7.0.12")).apply {
			withReuse(true)
		}
}
