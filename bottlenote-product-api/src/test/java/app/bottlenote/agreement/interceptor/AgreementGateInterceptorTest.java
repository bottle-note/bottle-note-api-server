package app.bottlenote.agreement.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.bottlenote.agreement.annotation.AgreementExempt;
import app.bottlenote.agreement.exception.AgreementException;
import app.bottlenote.agreement.exception.AgreementExceptionCode;
import app.bottlenote.agreement.fixture.FakeAgreementFacade;
import app.bottlenote.global.security.CustomUserContext;
import app.bottlenote.user.constant.UserType;
import app.bottlenote.user.domain.User;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

@Tag("unit")
@DisplayName("AgreementGateInterceptor 단위 테스트")
class AgreementGateInterceptorTest {

  private static final Long USER_ID = 10L;

  private FakeAgreementFacade agreementFacade;
  private AgreementGateInterceptor interceptor;
  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    agreementFacade = new FakeAgreementFacade();
    interceptor = new AgreementGateInterceptor(agreementFacade);
    request = new MockHttpServletRequest("GET", "/api/v1/users/current");
    response = new MockHttpServletResponse();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("비인증 요청은 동의 검사 없이 통과한다")
  void preHandle_whenUnauthenticated_passesWithoutEvaluation() throws Exception {
    HandlerMethod handler = handlerMethod(ProtectedHandler.class, "protectedAction");

    boolean allowed = interceptor.preHandle(request, response, handler);

    assertThat(allowed).isTrue();
  }

  @Test
  @DisplayName("인증 사용자가 동의를 충족하면 통과한다")
  void preHandle_whenAuthenticatedAndEligible_passes() throws Exception {
    authenticate(USER_ID);
    agreementFacade.setEligible(USER_ID, true);
    HandlerMethod handler = handlerMethod(ProtectedHandler.class, "protectedAction");

    boolean allowed = interceptor.preHandle(request, response, handler);

    assertThat(allowed).isTrue();
  }

  @Test
  @DisplayName("인증 사용자가 동의를 미충족하면 AGREEMENT_REQUIRED 예외를 던진다")
  void preHandle_whenAuthenticatedAndNotEligible_throwsAgreementRequired() throws Exception {
    authenticate(USER_ID);
    agreementFacade.setEligible(USER_ID, false);
    HandlerMethod handler = handlerMethod(ProtectedHandler.class, "protectedAction");

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handler))
        .isInstanceOf(AgreementException.class)
        .extracting(ex -> ((AgreementException) ex).getExceptionCode())
        .isEqualTo(AgreementExceptionCode.AGREEMENT_REQUIRED);
  }

  @Test
  @DisplayName("메서드 단위 면제 대상은 동의 미충족이어도 통과한다")
  void preHandle_whenMethodExempt_passesEvenIfNotEligible() throws Exception {
    authenticate(USER_ID);
    agreementFacade.setEligible(USER_ID, false);
    HandlerMethod handler = handlerMethod(ProtectedHandler.class, "methodExemptAction");

    assertThatCode(() -> interceptor.preHandle(request, response, handler))
        .doesNotThrowAnyException();
    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }

  @Test
  @DisplayName("클래스 단위 면제 대상은 동의 미충족이어도 통과한다")
  void preHandle_whenClassExempt_passesEvenIfNotEligible() throws Exception {
    authenticate(USER_ID);
    agreementFacade.setEligible(USER_ID, false);
    HandlerMethod handler = handlerMethod(ExemptHandler.class, "anyAction");

    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
  }

  @Test
  @DisplayName("HandlerMethod가 아니면 검사 없이 통과한다")
  void preHandle_whenHandlerIsNotHandlerMethod_passes() throws Exception {
    authenticate(USER_ID);
    agreementFacade.setEligible(USER_ID, false);

    assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
  }

  private void authenticate(Long userId) {
    User user =
        User.builder()
            .id(userId)
            .email("gate@test.com")
            .nickName("gate-user")
            .role(UserType.ROLE_USER)
            .build();
    CustomUserContext principal =
        new CustomUserContext(user, List.of(new SimpleGrantedAuthority(UserType.ROLE_USER.name())));
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
  }

  private HandlerMethod handlerMethod(Class<?> controllerType, String methodName) throws Exception {
    Object controller = controllerType.getDeclaredConstructor().newInstance();
    return new HandlerMethod(controller, controllerType.getMethod(methodName));
  }

  static class ProtectedHandler {
    public void protectedAction() {}

    @AgreementExempt
    public void methodExemptAction() {}
  }

  @AgreementExempt
  static class ExemptHandler {
    public void anyAction() {}
  }
}
