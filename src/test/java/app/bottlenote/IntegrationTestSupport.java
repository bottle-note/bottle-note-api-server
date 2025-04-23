package app.bottlenote;

import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.repository.OauthRepository;
import app.bottlenote.user.service.OauthService;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CompletableFuture;

@Testcontainers
@ActiveProfiles({"test", "batch"})
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("resource")
public abstract class IntegrationTestSupport {

	protected static final Logger log = LogManager.getLogger(IntegrationTestSupport.class);
	@Container
	protected static MySQLContainer<?> MY_SQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
			.withDatabaseName("bottlenote")
			.withUsername("root")
			.withPassword("root");
	@Container
	protected static GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7.0.12"))
			.withExposedPorts(6379);

	static {
		CompletableFuture<Void> mysqlFuture = CompletableFuture.runAsync(MY_SQL_CONTAINER::start);
		CompletableFuture<Void> redisFuture = CompletableFuture.runAsync(REDIS_CONTAINER::start);
		CompletableFuture.allOf(mysqlFuture, redisFuture).join();
	}

	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected OauthService oauthService;
	@Autowired
	protected OauthRepository oauthRepository;
	@Autowired
	private DataInitializer dataInitializer;
	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
		registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
		registry.add("spring.data.redis.password", () -> "");
	}

	@AfterEach
	void deleteAll() {
		log.info("데이터 초기화 dataInitializer.deleteAll() 시작");
		dataInitializer.deleteAll();
		log.info("데이터 초기화 dataInitializer.deleteAll() 종료");
	}

	protected TokenItem getToken(OauthRequest request) {
		return oauthService.login(request);
	}

	protected String getToken() {
		User user = oauthRepository.getFirstUser().orElseThrow(() -> new RuntimeException("init 처리된 유저가 없습니다."));
		TokenItem token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
		return token.accessToken();
	}

	protected Long getTokenUserId() {
		User user = oauthRepository.getFirstUser().orElseThrow(() -> new RuntimeException("init 처리된 유저가 없습니다."));
		return user.getId();
	}
}
