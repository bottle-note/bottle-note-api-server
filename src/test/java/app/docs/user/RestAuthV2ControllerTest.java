package app.docs.user;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.config.OauthConfigProperties;
import app.bottlenote.user.controller.AuthV2Controller;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.service.AuthService;
import app.bottlenote.user.service.NonceService;
import app.bottlenote.user.service.OauthService;
import app.docs.AbstractRestDocs;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;

@Tag("document")
@DisplayName("유저 Auth 컨트롤러 V2x RestDocs 테스트")
class RestAuthV2ControllerTest extends AbstractRestDocs {
  private final AuthService authService = mock(AuthService.class);
  private final NonceService nonceService = mock(NonceService.class);
  private final OauthService oauthService = mock(OauthService.class);
  private final OauthConfigProperties config = mock(OauthConfigProperties.class);

  private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

  @Override
  protected Object initController() {
    return new AuthV2Controller(authService, nonceService, oauthService, config);
  }

  @BeforeEach
  void setup() {
    mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
  }

  @AfterEach
  void tearDown() {
    mockedSecurityUtil.close();
  }

  @Test
  @DisplayName("루트 어드민 검증을 수행합니다.")
  void login_test() throws Exception {

    // given
    final long userId = 1L;
    when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));

    // when
    when(authService.checkAdminStatus(userId)).thenReturn(true);

    // then
    mockMvc
        .perform(
            get("/api/v2/auth/admin/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()))
        .andExpect(status().isOk())
        .andDo(
            document(
                "auth/admin/root-permissions",
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드(http status code)"),
                    fieldWithPath("data").description("검증 결과"),
                    fieldWithPath("errors").description("응답 성공 여부가 false일 경우 에러 메시지(없을 경우 null)"),
                    fieldWithPath("meta.serverEncoding").description("서버 인코딩 정도"),
                    fieldWithPath("meta.serverVersion").description("서버 버전"),
                    fieldWithPath("meta.serverPathVersion").description("서버 경로 버전"),
                    fieldWithPath("meta.serverResponseTime").description("서버 응답 시간"))));
  }

  @Test
  @DisplayName("Apple 로그인용 Nonce를 발급합니다.")
  void getAppleNonce_test() throws Exception {

    // given
    String nonce = "test-nonce-123";
    when(nonceService.generateNonce()).thenReturn(nonce);

    // then
    mockMvc
        .perform(
            get("/api/v2/auth/apple/nonce").contentType(MediaType.APPLICATION_JSON).with(csrf()))
        .andExpect(status().isOk())
        .andDo(
            document(
                "auth/apple/nonce",
                responseFields(fieldWithPath("nonce").description("Apple 로그인용 일회성 Nonce 값"))));
  }

  @Test
  @DisplayName("Apple 로그인을 수행합니다.")
  void executeAppleLogin_test() throws Exception {

    // given
    String idToken = "test-id-token";
    String nonce = "test-nonce";
    String accessToken = "test-access-token";
    String refreshToken = "test-refresh-token";

    TokenItem tokenItem =
        TokenItem.builder().accessToken(accessToken).refreshToken(refreshToken).build();

    when(oauthService.loginWithApple(anyString(), anyString())).thenReturn(tokenItem);

    Map<String, String> request = new HashMap<>();
    request.put("idToken", idToken);
    request.put("nonce", nonce);

    // then
    mockMvc
        .perform(
            post("/api/v2/auth/apple")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request))
                .with(csrf()))
        .andExpect(status().isOk())
        .andDo(
            document(
                "auth/apple/login",
                requestFields(
                    fieldWithPath("idToken").description("Apple에서 발급받은 ID 토큰"),
                    fieldWithPath("nonce").description("이전에 발급받은 Nonce 값")),
                responseFields(fieldWithPath("accessToken").description("발급된 액세스 토큰"))));
  }
}
