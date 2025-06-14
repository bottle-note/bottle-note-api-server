package app.bottlenote;

import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
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
	private static final Network network = Network.newNetwork();

	@Container
	protected static MySQLContainer<?> MY_SQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.0.32"))
			.withNetwork(network)
			.withDatabaseName("bottlenote")
			.withUsername("root")
			.withPassword("root");
	@Container
	protected static GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7.0.12"))
			.withExposedPorts(6379)
			.withNetworkAliases("redis")
			.withReuse(true)
			.withNetwork(network)
			.withStartupAttempts(5)
			.waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1))
			.withStartupTimeout(Duration.ofSeconds(30));

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
		registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379).toString());

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

	protected TokenItem getToken(User user) {
		OauthRequest req = new OauthRequest(user.getEmail(), null, user.getSocialType().getFirst(), user.getGender(), user.getAge());
		return oauthService.login(req);
	}

	protected String getToken() {
		User user = oauthRepository.getFirstUser().orElse(null);
		if (user == null) {
			UUID key = UUID.randomUUID();
			user = oauthRepository.save(User.builder()
					.email(key + "@example.com")
					.age(20)
					.gender(GenderType.MALE)
					.nickName("testUser" + key)
					.socialType(List.of(SocialType.KAKAO))
					.role(UserType.ROLE_USER)
					.build());
		}

		TokenItem token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
		return token.accessToken();
	}

	protected String getRandomToken() {
		UUID key = UUID.randomUUID();
		User user = oauthRepository.save(User.builder()
				.email(key + "@example.com")
				.age(20)
				.gender(GenderType.MALE)
				.nickName("testUser" + key)
				.socialType(List.of(SocialType.KAKAO))
				.role(UserType.ROLE_USER)
				.build());
		TokenItem token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole(), user.getId());
		return token.accessToken();
	}

	protected Long getTokenUserId() {
		User user = oauthRepository.getFirstUser().orElseThrow(() -> new RuntimeException("init 처리된 유저가 없습니다."));
		return user.getId();
	}

	protected Long getTokenUserId(String email) {
		User user = oauthRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("해당 이메일의 유저가 없습니다: " + email));
		return user.getId();
	}
}
