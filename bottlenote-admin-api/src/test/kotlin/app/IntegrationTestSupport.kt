package app

import app.helper.TestContainersConfig
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.assertj.MockMvcTester

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
}
