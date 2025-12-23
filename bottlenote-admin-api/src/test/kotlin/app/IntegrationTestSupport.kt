package app

import app.bottlenote.global.data.response.GlobalResponse
import app.bottlenote.global.security.jwt.JwtTokenProvider
import app.bottlenote.operation.utils.DataInitializer
import app.bottlenote.user.domain.AdminUser
import app.bottlenote.user.dto.response.TokenItem
import app.bottlenote.user.fixture.AdminUserTestFactory
import app.helper.TestContainersConfig
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.assertj.MvcTestResult

@Import(TestContainersConfig::class)
@ActiveProfiles("test")
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
abstract class IntegrationTestSupport {

	companion object {
		@JvmStatic
		protected val log: Logger = LogManager.getLogger(IntegrationTestSupport::class.java)
	}

	@Autowired
	protected lateinit var mapper: ObjectMapper

	@Autowired
	protected lateinit var mockMvcTester: MockMvcTester

	@Autowired
	protected lateinit var adminUserTestFactory: AdminUserTestFactory

	@Autowired
	protected lateinit var jwtTokenProvider: JwtTokenProvider

	@Autowired
	protected lateinit var dataInitializer: DataInitializer

	@AfterEach
	fun cleanUpAfterEach() {
		dataInitializer.deleteAll()
	}

	/**
	 * AdminUser에 대한 토큰 생성
	 */
	protected fun createToken(admin: AdminUser): TokenItem {
		return jwtTokenProvider.generateAdminToken(admin.email, admin.roles, admin.id)
	}

	/**
	 * AdminUser에 대한 액세스 토큰 문자열 반환
	 */
	protected fun getAccessToken(admin: AdminUser): String {
		return createToken(admin).accessToken()
	}

	/**
	 * MvcTestResult에서 GlobalResponse 파싱
	 */
	protected fun parseResponse(result: MvcTestResult): GlobalResponse {
		val responseString = result.response.contentAsString
		return mapper.readValue(responseString, GlobalResponse::class.java)
	}

	/**
	 * MvcTestResult에서 data 필드를 지정 타입으로 변환
	 */
	protected fun <T> extractData(result: MvcTestResult, dataType: Class<T>): T {
		val response = parseResponse(result)
		return mapper.convertValue(response.data, dataType)
	}
}
