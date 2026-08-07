package app.bottlenote.agreement.integration;

import static app.bottlenote.agreement.constant.AgreementAction.AGREE;
import static app.bottlenote.agreement.constant.AgreementAction.EXPIRED;
import static app.bottlenote.agreement.constant.AgreementInputContext.INDIVIDUAL;
import static app.bottlenote.agreement.constant.AgreementType.MARKETING;
import static app.bottlenote.agreement.constant.AgreementType.PRIVACY_COLLECTION_USE;
import static app.bottlenote.agreement.constant.AgreementType.TERMS_OF_SERVICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.agreement.domain.UserAgreement;
import app.bottlenote.agreement.domain.UserAgreementRepository;
import app.bottlenote.agreement.dto.request.AgreementSubmitRequest;
import app.bottlenote.agreement.dto.request.AgreementSubmitRequest.Item;
import app.bottlenote.agreement.exception.AgreementExceptionCode;
import app.bottlenote.global.exception.custom.code.ValidExceptionCode;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.fixture.UserTestFactory;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] [controller] AgreementController")
class AgreementControllerIntegrationTest extends IntegrationTestSupport {

  private static final String STATUS_ENDPOINT = "/api/v2/agreements/status";
  private static final String SUBMIT_ENDPOINT = "/api/v2/agreements";

  @Autowired private UserTestFactory userTestFactory;
  @Autowired private UserAgreementRepository userAgreementRepository;

  @Nested
  @DisplayName("동의 상태 조회")
  class GetStatus {

    @Test
    @DisplayName("인증 사용자는 동일한 필드의 동의 상태를 조회한다")
    void getStatus_whenAuthenticated_returnsExactStatusShape() throws Exception {
      User user = userTestFactory.persistUser();
      TokenItem token = getToken(user);

      MvcTestResult result =
          mockMvcTester
              .get()
              .uri(STATUS_ENDPOINT)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .exchange();

      result.assertThat().hasStatusOk();
      JsonNode data = responseData(result);
      assertThat(fieldNames(data)).containsExactlyInAnyOrder("eligible", "items");
      assertThat(data.path("eligible").booleanValue()).isFalse();
      assertThat(data.path("items")).hasSize(3);
      assertThat(fieldNames(data.path("items").get(0)))
          .containsExactlyInAnyOrder("type", "required", "agreed", "recordedAt");
      JsonNode marketing = item(data, MARKETING.name());
      assertThat(marketing.path("required").booleanValue()).isFalse();
      assertThat(marketing.path("agreed").booleanValue()).isFalse();
      assertThat(marketing.path("recordedAt").isNull()).isTrue();
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void getStatus_whenUnauthenticated_returnsUnauthorized() {
      mockMvcTester
          .get()
          .uri(STATUS_ENDPOINT)
          .exchange()
          .assertThat()
          .hasStatus(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("동의 제출")
  class Submit {

    @Test
    @DisplayName("인증 사용자의 원문과 요청 메타데이터를 저장하고 갱신된 상태를 반환한다")
    void submit_whenValidRequest_savesEvidenceAndReturnsStatus() throws Exception {
      User user = userTestFactory.persistUser();
      TokenItem token = getToken(user);
      AgreementSubmitRequest request = validRequest();

      MvcTestResult result =
          mockMvcTester
              .post()
              .uri(SUBMIT_ENDPOINT)
              .contentType(APPLICATION_JSON)
              .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
              .header("X-Forwarded-For", "203.0.113.10")
              .header(HttpHeaders.USER_AGENT, "BottleNote/2.0")
              .content(mapper.writeValueAsString(request))
              .with(csrf())
              .exchange();

      result.assertThat().hasStatusOk();
      JsonNode data = responseData(result);
      assertThat(fieldNames(data)).containsExactlyInAnyOrder("eligible", "items");
      assertThat(data.path("eligible").booleanValue()).isTrue();
      assertThat(data.path("items")).hasSize(3);
      assertThat(fieldNames(data.path("items").get(0)))
          .containsExactlyInAnyOrder("type", "required", "agreed", "recordedAt");
      JsonNode marketing = item(data, MARKETING.name());
      assertThat(marketing.path("required").booleanValue()).isFalse();
      assertThat(marketing.path("agreed").booleanValue()).isTrue();
      assertThat(marketing.path("recordedAt").isNull()).isFalse();

      UserAgreement terms = latest(user, TERMS_OF_SERVICE);
      assertThat(terms.getAction()).isEqualTo(AGREE);
      assertThat(terms.getDocumentContent()).isEqualTo("이용약관 원문");
      assertThat(terms.getInputContext()).isEqualTo(INDIVIDUAL);
      assertThat(terms.getClientIp()).isEqualTo("203.0.113.10");
      assertThat(terms.getUserAgent()).isEqualTo("BottleNote/2.0");
      assertThat(latest(user, MARKETING).getAction()).isEqualTo(AGREE);
      assertThat(item(data, TERMS_OF_SERVICE.name()).path("recordedAt").asText())
          .isEqualTo(terms.getRecordedAt().toString());
    }

    @Test
    @DisplayName("비어 있는 원문을 제출하면 400을 반환한다")
    void submit_whenContentIsBlank_returnsBadRequest() throws Exception {
      User user = userTestFactory.persistUser();
      TokenItem token = getToken(user);
      AgreementSubmitRequest request =
          new AgreementSubmitRequest(
              List.of(new Item(TERMS_OF_SERVICE.name(), AGREE, " ", INDIVIDUAL)));

      MvcTestResult result = submit(token, request);

      result.assertThat().hasStatus(HttpStatus.BAD_REQUEST);
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.errors[0].code")
          .isEqualTo(ValidExceptionCode.AGREEMENT_CONTENT_EMPTY.name());
    }

    @Test
    @DisplayName("알 수 없는 동의 유형을 제출하면 400을 반환한다")
    void submit_whenAgreementTypeIsUnknown_returnsBadRequest() throws Exception {
      User user = userTestFactory.persistUser();
      TokenItem token = getToken(user);
      AgreementSubmitRequest request =
          new AgreementSubmitRequest(List.of(new Item("UNKNOWN", AGREE, "원문", INDIVIDUAL)));

      MvcTestResult result = submit(token, request);

      result.assertThat().hasStatus(HttpStatus.BAD_REQUEST);
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.errors[0].code")
          .isEqualTo(AgreementExceptionCode.INVALID_AGREEMENT_TYPE.name());
    }

    @Test
    @DisplayName("만료 상태를 제출하면 저장하지 않고 400을 반환한다")
    void submit_whenActionIsExpired_returnsBadRequestWithoutSaving() throws Exception {
      User user = userTestFactory.persistUser();
      TokenItem token = getToken(user);
      AgreementSubmitRequest request =
          new AgreementSubmitRequest(
              List.of(new Item(TERMS_OF_SERVICE.name(), EXPIRED, "원문", INDIVIDUAL)));

      MvcTestResult result = submit(token, request);

      result.assertThat().hasStatus(HttpStatus.BAD_REQUEST);
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.errors[0].code")
          .isEqualTo(AgreementExceptionCode.UNSUPPORTED_AGREEMENT_ACTION.name());
      assertThat(
              userAgreementRepository.findFirstByUserIdAndAgreementTypeOrderByIdDesc(
                  user.getId(), TERMS_OF_SERVICE))
          .isEmpty();
    }

    @Test
    @DisplayName("동의 항목이 비어 있으면 400을 반환한다")
    void submit_whenAgreementsAreEmpty_returnsBadRequest() throws Exception {
      User user = userTestFactory.persistUser();
      TokenItem token = getToken(user);

      MvcTestResult result = submit(token, new AgreementSubmitRequest(List.of()));

      result.assertThat().hasStatus(HttpStatus.BAD_REQUEST);
      result
          .assertThat()
          .bodyJson()
          .extractingPath("$.errors[0].code")
          .isEqualTo(ValidExceptionCode.AGREEMENTS_REQUIRED.name());
    }

    @Test
    @DisplayName("인증 정보가 없으면 401을 반환한다")
    void submit_whenUnauthenticated_returnsUnauthorized() throws Exception {
      mockMvcTester
          .post()
          .uri(SUBMIT_ENDPOINT)
          .contentType(APPLICATION_JSON)
          .content(mapper.writeValueAsString(validRequest()))
          .with(csrf())
          .exchange()
          .assertThat()
          .hasStatus(HttpStatus.UNAUTHORIZED);
    }
  }

  private MvcTestResult submit(TokenItem token, AgreementSubmitRequest request) throws Exception {
    return mockMvcTester
        .post()
        .uri(SUBMIT_ENDPOINT)
        .contentType(APPLICATION_JSON)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
        .content(mapper.writeValueAsString(request))
        .with(csrf())
        .exchange();
  }

  private AgreementSubmitRequest validRequest() {
    return new AgreementSubmitRequest(
        List.of(
            new Item(TERMS_OF_SERVICE.name(), AGREE, "이용약관 원문", INDIVIDUAL),
            new Item(PRIVACY_COLLECTION_USE.name(), AGREE, "개인정보 수집 이용 원문", INDIVIDUAL),
            new Item(MARKETING.name(), AGREE, "마케팅 정보 수신 동의 원문", INDIVIDUAL)));
  }

  private UserAgreement latest(User user, app.bottlenote.agreement.constant.AgreementType type) {
    return userAgreementRepository
        .findFirstByUserIdAndAgreementTypeOrderByIdDesc(user.getId(), type)
        .orElseThrow();
  }

  private JsonNode responseData(MvcTestResult result) throws Exception {
    return mapper.readTree(result.getResponse().getContentAsString()).path("data");
  }

  private Set<String> fieldNames(JsonNode node) {
    return StreamSupport.stream(((Iterable<String>) () -> node.fieldNames()).spliterator(), false)
        .collect(java.util.stream.Collectors.toSet());
  }

  private JsonNode item(JsonNode data, String type) {
    return StreamSupport.stream(data.path("items").spliterator(), false)
        .filter(item -> item.path("type").asText().equals(type))
        .findFirst()
        .orElseThrow();
  }
}
