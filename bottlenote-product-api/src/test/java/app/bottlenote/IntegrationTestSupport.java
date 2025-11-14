package app.bottlenote;

import app.bottlenote.global.data.response.GlobalResponse;
import app.bottlenote.operation.utils.TestAuthenticationSupport;
import app.bottlenote.operation.utils.TestContainersConfig;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.request.OauthRequest;
import app.bottlenote.user.dto.response.TokenItem;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  @Autowired protected TestAuthenticationSupport authSupport;
  @Autowired protected DataInitializer dataInitializer;

  @AfterEach
  void cleanUpAfterEach() {
    dataInitializer.deleteAll();
  }

  // ========== 인증 관련 메서드 (위임) ==========

  protected TokenItem getToken(OauthRequest request) {
    return authSupport.createToken(request);
  }

  protected TokenItem getToken(User user) {
    return authSupport.createToken(user);
  }

  protected String getToken() {
    return authSupport.getAccessToken();
  }

  protected Long getTokenUserId() {
    return authSupport.getDefaultUserId();
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
