package app.docs.agreement;

import static app.bottlenote.agreement.constant.AgreementAction.AGREE;
import static app.bottlenote.agreement.constant.AgreementInputContext.INDIVIDUAL;
import static app.bottlenote.agreement.constant.AgreementType.MARKETING;
import static app.bottlenote.agreement.constant.AgreementType.PRIVACY_COLLECTION_USE;
import static app.bottlenote.agreement.constant.AgreementType.TERMS_OF_SERVICE;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.agreement.controller.AgreementController;
import app.bottlenote.agreement.dto.request.AgreementSubmitRequest;
import app.bottlenote.agreement.dto.request.AgreementSubmitRequest.Item;
import app.bottlenote.agreement.fixture.InMemoryUserAgreementRepository;
import app.bottlenote.agreement.service.AgreementEvaluator;
import app.bottlenote.agreement.service.AgreementService;
import app.docs.AbstractRestDocs;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@Tag("restdocs")
@DisplayName("사용자 동의 API 문서화 테스트")
class RestAgreementControllerDocsTest extends AbstractRestDocs {

  private static final Long USER_ID = 1L;

  private final InMemoryUserAgreementRepository agreementRepository =
      new InMemoryUserAgreementRepository();
  private final AgreementService agreementService =
      new AgreementService(
          agreementRepository,
          new AgreementEvaluator(agreementRepository),
          Clock.fixed(Instant.parse("2026-08-01T09:00:00Z"), ZoneOffset.UTC));

  @Override
  protected Object initController() {
    return new AgreementController(agreementService);
  }

  @BeforeEach
  void setAuthentication() {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken(USER_ID.toString(), null));
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("동의 상태 조회 응답의 exact field를 문서화한다")
  void getStatus() throws Exception {
    mockMvc
        .perform(get("/api/v2/agreements/status"))
        .andExpect(status().isOk())
        .andDo(document("agreements/status", responseFields(statusResponseFields())));
  }

  @Test
  @DisplayName("동의 제출 요청과 응답의 exact field를 문서화한다")
  void submit() throws Exception {
    AgreementSubmitRequest request =
        new AgreementSubmitRequest(
            List.of(
                new Item(TERMS_OF_SERVICE.name(), AGREE, "이용약관 원문", INDIVIDUAL),
                new Item(PRIVACY_COLLECTION_USE.name(), AGREE, "개인정보 수집 이용 원문", INDIVIDUAL),
                new Item(MARKETING.name(), AGREE, "마케팅 정보 수신 동의 원문", INDIVIDUAL)));
    mockMvc
        .perform(
            post("/api/v2/agreements")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "203.0.113.10")
                .header(HttpHeaders.USER_AGENT, "BottleNote/2.0")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andDo(
            document(
                "agreements/submit",
                requestFields(
                    fieldWithPath("agreements").type(ARRAY).description("제출할 동의 항목"),
                    fieldWithPath("agreements[].type")
                        .type(STRING)
                        .description(
                            "동의 유형: TERMS_OF_SERVICE, PRIVACY_COLLECTION_USE 또는 MARKETING"),
                    fieldWithPath("agreements[].action")
                        .type(STRING)
                        .description("사용자 제출 가능 동작: AGREE 또는 REVOKE. EXPIRED는 서버 전용 상태로 제출할 수 없음"),
                    fieldWithPath("agreements[].content")
                        .type(STRING)
                        .description("사용자에게 표시한 문서 원문 스냅샷"),
                    fieldWithPath("agreements[].inputContext")
                        .type(STRING)
                        .description("선택 맥락: INDIVIDUAL 또는 BULK")),
                responseFields(statusResponseFields())));
  }

  private org.springframework.restdocs.payload.FieldDescriptor[] statusResponseFields() {
    return new org.springframework.restdocs.payload.FieldDescriptor[] {
      fieldWithPath("success").type(BOOLEAN).description("응답 성공 여부"),
      fieldWithPath("code").type(NUMBER).description("HTTP 응답 코드"),
      fieldWithPath("data").type(OBJECT).description("동의 상태"),
      fieldWithPath("data.eligible").type(BOOLEAN).description("모든 필수 동의 충족 여부"),
      fieldWithPath("data.items").type(ARRAY).description("유형별 동의 상태"),
      fieldWithPath("data.items[].type").type(STRING).description("동의 유형"),
      fieldWithPath("data.items[].required").type(BOOLEAN).description("필수 동의 여부"),
      fieldWithPath("data.items[].agreed").type(BOOLEAN).description("현재 동의 충족 여부"),
      fieldWithPath("errors").type(ARRAY).description("에러 목록"),
      fieldWithPath("meta.serverEncoding").ignored(),
      fieldWithPath("meta.serverVersion").ignored(),
      fieldWithPath("meta.serverPathVersion").ignored(),
      fieldWithPath("meta.serverResponseTime").ignored()
    };
  }
}
