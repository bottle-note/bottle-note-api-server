package app.bottlenote;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.global.security.jwt.JwtTokenProvider;
import app.bottlenote.operation.utils.TestContainersConfig;
import app.bottlenote.user.constant.GenderType;
import app.bottlenote.user.constant.SocialType;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.repository.OauthRepository;
import app.bottlenote.user.service.OauthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Import(TestContainersConfig.class)
@ActiveProfiles({"test", "batch"})
@Tag("integration")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class IntegrationTestSupport {

  protected static final Logger log = LogManager.getLogger(IntegrationTestSupport.class);

  @Autowired protected ObjectMapper mapper;
  @Autowired protected MockMvc mockMvc;
  @Autowired protected MockMvcTester mockMvcTester;
  @Autowired protected OauthService oauthService;
  @Autowired protected OauthRepository oauthRepository;
  @Autowired private DataInitializer dataInitializer;
  @Autowired private JwtTokenProvider jwtTokenProvider;

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
    OauthRequest req =
        new OauthRequest(
            user.getEmail(),
            null,
            user.getSocialType().getFirst(),
            user.getGender(),
            user.getAge());
    return oauthService.login(req);
  }

  protected String getToken() {
    User user = oauthRepository.getFirstUser().orElse(null);
    if (user == null) {
      UUID key = UUID.randomUUID();
      user =
          oauthRepository.save(
              User.builder()
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
    User user =
        oauthRepository.save(
            User.builder()
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
    User user =
        oauthRepository
            .getFirstUser()
            .orElseThrow(() -> new RuntimeException("init 처리된 유저가 없습니다."));
    return user.getId();
  }

  protected Long getTokenUserId(String email) {
    User user =
        oauthRepository
            .findByEmail(email)
            .orElseThrow(() -> new RuntimeException("해당 이메일의 유저가 없습니다: " + email));
    return user.getId();
  }

  /**
   * MvcTestResult에서 GlobalResponse를 파싱하고 data 필드를 지정된 타입으로 변환
   *
   * @param result MvcTestResult (MockMvcTester.exchange()의 결과)
   * @param dataType 변환할 data 필드의 타입
   * @param <T> 반환 타입
   * @return GlobalResponse의 data를 지정된 타입으로 변환한 객체
   * @throws Exception JSON 파싱 실패 시
   */
  protected <T> T extractData(MvcTestResult result, Class<T> dataType) throws Exception {
    result.assertThat().hasStatusOk();
    String responseString = result.getResponse().getContentAsString();
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    return mapper.convertValue(response.getData(), dataType);
  }

  /**
   * MvcResult에서 GlobalResponse를 파싱하고 data 필드를 지정된 타입으로 변환 (레거시 MockMvc 지원)
   *
   * @param result MvcResult (MockMvc.perform().andReturn()의 결과)
   * @param dataType 변환할 data 필드의 타입
   * @param <T> 반환 타입
   * @return GlobalResponse의 data를 지정된 타입으로 변환한 객체
   * @throws Exception JSON 파싱 실패 시
   */
  protected <T> T extractData(MvcResult result, Class<T> dataType) throws Exception {
    String responseString = result.getResponse().getContentAsString();
    GlobalResponse response = mapper.readValue(responseString, GlobalResponse.class);
    return mapper.convertValue(response.getData(), dataType);
  }

  /**
   * MvcTestResult에서 GlobalResponse만 파싱 (data 변환 없이)
   *
   * @param result MvcTestResult (MockMvcTester.exchange()의 결과)
   * @return GlobalResponse 객체
   * @throws Exception JSON 파싱 실패 시
   */
  protected GlobalResponse parseResponse(MvcTestResult result) throws Exception {
    result.assertThat().hasStatusOk();
    String responseString = result.getResponse().getContentAsString();
    return mapper.readValue(responseString, GlobalResponse.class);
  }

  /**
   * MvcResult에서 GlobalResponse만 파싱 (레거시 MockMvc 지원)
   *
   * @param result MvcResult (MockMvc.perform().andReturn()의 결과)
   * @return GlobalResponse 객체
   * @throws Exception JSON 파싱 실패 시
   */
  protected GlobalResponse parseResponse(MvcResult result) throws Exception {
    String responseString = result.getResponse().getContentAsString();
    return mapper.readValue(responseString, GlobalResponse.class);
  }
}
