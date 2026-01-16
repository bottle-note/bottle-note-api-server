package app.docs.alcohols;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.alcohols.controller.TastingTagController;
import app.bottlenote.alcohols.service.TastingTagService;
import app.external.docs.AbstractRestDocs;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

@Tag("rest-docs")
@DisplayName("TastingTag API 문서화 테스트")
class RestTastingTagControllerTest extends AbstractRestDocs {

  private final TastingTagService tastingTagService = mock(TastingTagService.class);

  @Override
  protected Object initController() {
    return new TastingTagController(tastingTagService);
  }

  @Test
  @DisplayName("문장에서 테이스팅 태그를 추출할 수 있다")
  void getExtractedTags() throws Exception {
    // given
    List<String> tags = List.of("바닐라", "꿀", "스모키");
    when(tastingTagService.extractTagNames(anyString())).thenReturn(tags);

    // when
    ResultActions resultActions =
        mockMvc.perform(
            get("/api/v1/tasting-tags/extract").param("text", "바닐라 향이 좋고 꿀 같은 단맛에 스모키 함이 느껴져요"));

    // then
    resultActions
        .andExpect(status().isOk())
        .andDo(
            document(
                "tasting-tags-extract",
                queryParameters(parameterWithName("text").description("태그를 추출할 문장 (리뷰 내용 등)")),
                responseFields(
                    fieldWithPath("success").type(BOOLEAN).description("성공 여부"),
                    fieldWithPath("code").type(NUMBER).description("응답 코드"),
                    fieldWithPath("data").type(ARRAY).description("추출된 태그 이름 목록 (문자열 배열)"),
                    fieldWithPath("meta").type(OBJECT).description("메타 정보"),
                    fieldWithPath("meta.serverVersion").type(STRING).description("서버 버전"),
                    fieldWithPath("meta.serverEncoding").type(STRING).description("서버 인코딩"),
                    fieldWithPath("meta.serverResponseTime").type(ARRAY).description("응답 시간"),
                    fieldWithPath("meta.serverPathVersion").type(STRING).description("API 경로 버전"),
                    fieldWithPath("errors").description("에러 정보").optional())));
  }
}
