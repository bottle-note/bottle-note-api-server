package app.docs.user;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.config.OauthConfigProperties;
import app.bottlenote.user.controller.AuthV2Controller;
import app.bottlenote.user.service.AuthService;
import app.bottlenote.user.service.NonceService;
import app.docs.AbstractRestDocs;
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper;
import com.epages.restdocs.apispec.ResourceDocumentation;
import com.epages.restdocs.apispec.ResourceSnippetParameters;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.PayloadDocumentation;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@Tag("document")
@DisplayName("유저 Auth 컨트롤러 V2x OpenAPI 테스트")
class OpenApiAuthV2ControllerTest extends AbstractRestDocs {
  private final AuthService authService = Mockito.mock(AuthService.class);
  private final NonceService nonceService = Mockito.mock(NonceService.class);
  private final OauthConfigProperties config = Mockito.mock(OauthConfigProperties.class);

  private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

  @Override
  protected Object initController() {
    return new AuthV2Controller(authService, nonceService, config);
  }

  @BeforeEach
  void setup() {
    mockedSecurityUtil = Mockito.mockStatic(SecurityContextUtil.class);
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
    Mockito.when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));
    Mockito.when(authService.checkAdminStatus(userId)).thenReturn(true);

    // when & then
    mockMvc
        .perform(
            RestDocumentationRequestBuilders.get("/api/v2/auth/admin/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .with(SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andDo(
            MockMvcRestDocumentationWrapper.document(
                "auth-admin-permissions",
                ResourceDocumentation.resource(
                    ResourceSnippetParameters.builder()
                        .tag("Authentication")
                        .summary("관리자 권한 확인")
                        .description("사용자의 관리자 권한을 확인합니다")
                        .responseFields(
                            PayloadDocumentation.fieldWithPath("success").description("응답 성공 여부"),
                            PayloadDocumentation.fieldWithPath("code").description("응답 코드"),
                            PayloadDocumentation.fieldWithPath("data").description("검증 결과"),
                            PayloadDocumentation.fieldWithPath("errors")
                                .description("에러 메시지")
                                .optional(),
                            PayloadDocumentation.fieldWithPath("meta.serverEncoding")
                                .description("서버 인코딩"),
                            PayloadDocumentation.fieldWithPath("meta.serverVersion")
                                .description("서버 버전"),
                            PayloadDocumentation.fieldWithPath("meta.serverPathVersion")
                                .description("서버 경로 버전"),
                            PayloadDocumentation.fieldWithPath("meta.serverResponseTime")
                                .description("서버 응답 시간"))
                        .build())));
  }
}
