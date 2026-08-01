package app.docs.user;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.config.OauthConfigProperties;
import app.bottlenote.user.controller.AuthV2Controller;
import app.bottlenote.user.dto.response.AuthResponse;
import app.bottlenote.user.dto.response.TokenItem;
import app.bottlenote.user.service.AuthService;
import app.bottlenote.user.service.NonceService;
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
import org.springframework.restdocs.payload.JsonFieldType;

@Tag("restdocs")
@DisplayName("유저 Auth 컨트롤러 V2x RestDocs 테스트")
class RestAuthV2ControllerTest extends AbstractRestDocs {
  private final AuthService authService = mock(AuthService.class);
  private final NonceService nonceService = mock(NonceService.class);
  private final OauthConfigProperties config = mock(OauthConfigProperties.class);

  private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

  @Override
  protected Object initController() {
    return new AuthV2Controller(authService, nonceService, config);
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

    AuthResponse authResult = new AuthResponse(tokenItem, true, "부드러운몰트1234", true);

    when(authService.loginWithApple(anyString(), anyString())).thenReturn(authResult);

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
        .andExpect(cookie().value("refresh-token", refreshToken))
        .andExpect(jsonPath("$.agreementRequired").value(true))
        .andDo(
            document(
                "auth/apple/login",
                requestFields(
                    fieldWithPath("idToken").description("Apple에서 발급받은 ID 토큰"),
                    fieldWithPath("nonce").description("이전에 발급받은 Nonce 값")),
                responseFields(
                    fieldWithPath("accessToken").description("발급된 액세스 토큰"),
                    fieldWithPath("isFirstLogin")
                        .description("최초 로그인 여부 (true: 최초 로그인, false: 기존 사용자)")
                        .optional(),
                    fieldWithPath("nickname")
                        .description("사용자 닉네임 (최초 로그인 시 자동 생성된 닉네임)")
                        .optional(),
                    fieldWithPath("agreementRequired").description("필수 동의가 필요하면 true"))));
  }

  @Test
  @DisplayName("카카오 로그인을 수행합니다.")
  void executeKakaoLogin_test() throws Exception {

    // given
    String kakaoAccessToken = "test-kakao-access-token";
    String accessToken = "test-access-token";
    String refreshToken = "test-refresh-token";

    TokenItem tokenItem =
        TokenItem.builder().accessToken(accessToken).refreshToken(refreshToken).build();

    AuthResponse authResult = new AuthResponse(tokenItem, true, "부드러운몰트1234", true);

    when(authService.loginWithKakao(anyString())).thenReturn(authResult);

    Map<String, String> request = new HashMap<>();
    request.put("accessToken", kakaoAccessToken);

    // then
    mockMvc
        .perform(
            post("/api/v2/auth/kakao")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(cookie().value("refresh-token", refreshToken))
        .andExpect(jsonPath("$.agreementRequired").value(true))
        .andDo(
            document(
                "auth/kakao/login",
                requestFields(fieldWithPath("accessToken").description("카카오에서 발급받은 액세스 토큰")),
                responseFields(
                    fieldWithPath("accessToken").description("발급된 액세스 토큰"),
                    fieldWithPath("isFirstLogin")
                        .description("최초 로그인 여부 (true: 최초 로그인, false: 기존 사용자)")
                        .optional(),
                    fieldWithPath("nickname")
                        .description("사용자 닉네임 (최초 로그인 시 자동 생성된 닉네임)")
                        .optional(),
                    fieldWithPath("agreementRequired").description("필수 동의가 필요하면 true"))));
  }

  @Test
  @DisplayName("에이전트 키로 로그인을 수행합니다.")
  void executeAgentLogin_test() throws Exception {

    // given
    String agentKey = "bn_agent_" + "A".repeat(43);
    String accessToken = "test-access-token";
    String refreshToken = "test-refresh-token";

    TokenItem tokenItem =
        TokenItem.builder().accessToken(accessToken).refreshToken(refreshToken).build();

    AuthResponse authResult = new AuthResponse(tokenItem, false, "agent_user_0001", true);

    when(authService.loginWithAgent(anyString())).thenReturn(authResult);

    Map<String, String> request = new HashMap<>();
    request.put("agentKey", agentKey);

    // then
    mockMvc
        .perform(
            post("/api/v2/auth/agent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("refresh-token"))
        .andDo(
            document(
                "auth/agent/login",
                requestFields(fieldWithPath("agentKey").description("발급받은 에이전트 비밀 API Key")),
                responseFields(
                    fieldWithPath("accessToken").description("발급된 액세스 토큰"),
                    fieldWithPath("isFirstLogin")
                        .description("최초 로그인 여부 (true: 최초 로그인, false: 기존 사용자)")
                        .optional(),
                    fieldWithPath("nickname").description("사용자 닉네임").optional(),
                    fieldWithPath("agreementRequired").description("필수 동의가 필요하면 true"))));
  }

  @Test
  @DisplayName("토큰을 재발급할 수 있다.")
  void reissue_on_v2_test() throws Exception {
    // given
    String refreshToken = "refresh-token";
    TokenItem newToken =
        TokenItem.builder().accessToken("new-access-token").refreshToken("new-refresh").build();
    when(authService.reissue(refreshToken)).thenReturn(newToken);

    // then
    mockMvc
        .perform(
            post("/api/v2/auth/reissue")
                .header("refresh-token", refreshToken)
                .contentType(MediaType.APPLICATION_JSON)
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(cookie().exists("refresh-token"))
        .andDo(
            document(
                "user/user-reissue",
                responseFields(
                    fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
                    fieldWithPath("code")
                        .type(JsonFieldType.NUMBER)
                        .description("응답 코드(http status code)"),
                    fieldWithPath("data.accessToken")
                        .type(JsonFieldType.STRING)
                        .description("액세스 토큰"),
                    fieldWithPath("data.isFirstLogin")
                        .type(JsonFieldType.BOOLEAN)
                        .description("최초 로그인 여부 (재발급 시에는 내려가지 않음)")
                        .optional(),
                    fieldWithPath("data.nickname")
                        .type(JsonFieldType.STRING)
                        .description("사용자 닉네임 (재발급 시에는 내려가지 않음)")
                        .optional(),
                    fieldWithPath("data.agreementRequired")
                        .type(JsonFieldType.BOOLEAN)
                        .description("필수 동의 필요 여부 (재발급 시에는 내려가지 않음)")
                        .optional(),
                    fieldWithPath("errors")
                        .type(JsonFieldType.ARRAY)
                        .description("응답 성공 여부가 false일 경우 에러 메시지(없을 경우 null)"),
                    fieldWithPath("meta.serverEncoding").description("서버 인코딩 정도"),
                    fieldWithPath("meta.serverVersion").description("서버 버전"),
                    fieldWithPath("meta.serverPathVersion").description("서버 경로 버전"),
                    fieldWithPath("meta.serverResponseTime").description("서버 응답 시간")),
                responseHeaders(headerWithName("Set-Cookie").description("리프레쉬 토큰"))));
  }

  @Test
  @DisplayName("토큰 유효성을 검사할 수 있다.")
  void verify_on_v2_test() throws Exception {
    // given
    when(authService.verifyToken("test-token")).thenReturn("Token is valid");

    Map<String, String> request = new HashMap<>();
    request.put("token", "test-token");

    // then
    mockMvc
        .perform(
            put("/api/v2/auth/token/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request))
                .with(csrf()))
        .andExpect(status().isOk())
        .andDo(
            document(
                "user/token-verify",
                requestFields(fieldWithPath("token").description("검사할 토큰")),
                responseFields(
                    fieldWithPath("success").ignored(),
                    fieldWithPath("code").ignored(),
                    fieldWithPath("errors").ignored(),
                    fieldWithPath("data").description("결과"),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored())));
  }
}
