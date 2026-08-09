package app.global.security

import app.IntegrationTestSupport
import app.bottlenote.accesscontrol.facade.IpBanFacade
import app.bottlenote.global.security.accesscontrol.AccessControlStore
import app.bottlenote.global.security.accesscontrol.fixture.InMemoryAccessControlStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.util.UUID

@Tag("admin_integration")
@DisplayName("[integration] Admin IP 차단 projection 보류 응답")
@ActiveProfiles("test", "access-control-it")
@Import(AccessControlPendingProjectionIntegrationTest.FailingProjectionStoreConfiguration::class)
class AccessControlPendingProjectionIntegrationTest : IntegrationTestSupport() {
	private lateinit var accessToken: String

	@org.junit.jupiter.api.BeforeEach
	fun setUpAdminToken() {
		accessToken = getAccessToken(adminUserTestFactory.persistRootAdmin())
	}

	@org.springframework.beans.factory.annotation.Autowired
	private lateinit var ipBanFacade: IpBanFacade

	@Test
	@DisplayName("Redis projection 실패면 202과 PENDING을 반환하고 DB 상태와 event를 보존한다")
	fun projectionFailureReturns202AndPreservesDatabaseState() {
		val targetIp = nextTestIp()
		val result =
			mockMvcTester
				.post()
				.uri("/v1/access-control/ip-bans")
				.header("Authorization", "Bearer $accessToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(mapOf("ip" to targetIp, "ttlSeconds" to 60, "reason" to "projection-failure")))
				.exchange()

		result.assertThat().hasStatus(HttpStatus.ACCEPTED)
		assertThat(mapper.readTree(result.response.contentAsString).path("data").path("projectionStatus").asText())
			.isEqualTo("PENDING_RECONCILE")
		val detail = ipBanFacade.findByIp(targetIp).orElseThrow()
		assertThat(detail.status().name).isEqualTo("ACTIVE")
		assertThat(detail.events()).hasSize(1)
		assertThat(detail.events().single().reason()).isEqualTo("projection-failure")
	}

	private fun nextTestIp(): String {
		val uuid = UUID.randomUUID()
		val c = Math.floorMod(uuid.mostSignificantBits.toInt(), 254) + 1
		val d = Math.floorMod(uuid.leastSignificantBits.toInt(), 254) + 1
		return "198.19.$c.$d"
	}

	@TestConfiguration(proxyBeanMethods = false)
	class FailingProjectionStoreConfiguration {
		@Bean
		@Primary
		fun failingProjectionStore(): AccessControlStore = object : AccessControlStore by InMemoryAccessControlStore() {
			override fun projectBan(ip: String, ttl: Duration, reason: String, eventId: Long): Unit = throw IllegalStateException("test projection failure")

			override fun projectUnban(ip: String, eventId: Long): Unit = throw IllegalStateException("test projection failure")
		}
	}
}
