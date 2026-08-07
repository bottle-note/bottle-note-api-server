package app.bottlenote.agreement.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.agreement.exception.AgreementExceptionCode;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.fixture.UserTestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@Tag("integration")
@DisplayName("[integration] AgreementGate 스모크")
class AgreementGateIntegrationTest extends IntegrationTestSupport {

  private static final String PROTECTED_ENDPOINT = "/api/v1/users/current";
  private static final String STATUS_ENDPOINT = "/api/v2/agreements/status";
  private static final String WITHDRAW_ENDPOINT = "/api/v1/users";

  @Autowired private UserTestFactory userTestFactory;

  @Test
  @DisplayName("동의 미충족 인증 사용자는 보호 API에서 AGREEMENT_REQUIRED를 받는다")
  void protectedApi_whenAgreementMissing_returnsAgreementRequired() {
    User user = userTestFactory.persistUserWithoutAgreements();
    TokenItem token = getToken(user);

    MvcTestResult result =
        mockMvcTester
            .get()
            .uri(PROTECTED_ENDPOINT)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
            .exchange();

    result.assertThat().hasStatus(HttpStatus.FORBIDDEN);
    result
        .assertThat()
        .bodyJson()
        .extractingPath("$.errors[0].code")
        .isEqualTo(AgreementExceptionCode.AGREEMENT_REQUIRED.name());
  }

  @Test
  @DisplayName("동의 미충족이어도 동의 상태 조회와 탈퇴는 면제되어 통과한다")
  void exemptApis_whenAgreementMissing_areAccessible() {
    User user = userTestFactory.persistUserWithoutAgreements();
    TokenItem token = getToken(user);

    mockMvcTester
        .get()
        .uri(STATUS_ENDPOINT)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
        .exchange()
        .assertThat()
        .hasStatusOk();

    mockMvcTester
        .delete()
        .uri(WITHDRAW_ENDPOINT)
        .contentType(APPLICATION_JSON)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
        .with(csrf())
        .exchange()
        .assertThat()
        .hasStatusOk();
  }

  @Test
  @DisplayName("필수 동의 충족 사용자는 보호 API를 호출할 수 있다")
  void protectedApi_whenEligible_returnsOk() throws Exception {
    User user = userTestFactory.persistUser();
    TokenItem token = getToken(user);

    MvcTestResult result =
        mockMvcTester
            .get()
            .uri(PROTECTED_ENDPOINT)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
            .exchange();

    result.assertThat().hasStatusOk();
    assertThat(result.getResponse().getContentAsString()).contains("nickname");
  }
}
