package app.bottlenote;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class IntegrationTestSupport {
	protected static final Logger log = LogManager.getLogger(IntegrationTestSupport.class);
	protected static MySQLContainer<?> MY_SQL_CONTAINER;

	static {
		MY_SQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
			.withDatabaseName("bottlenote")
			.withUsername("root")
			.withPassword("root");

		MY_SQL_CONTAINER.start();
	}

	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	private DataInitializer dataInitializer;

	@AfterEach
	void deleteAll() {
		log.info("데이터 초기화 dataInitializer.deleteAll() 시작");
		dataInitializer.deleteAll();
		log.info("데이터 초기화 dataInitializer.deleteAll() 종료");
	}
}
