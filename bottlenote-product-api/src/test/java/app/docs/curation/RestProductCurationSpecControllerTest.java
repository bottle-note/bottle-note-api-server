package app.docs.curation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.BOOLEAN;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.OBJECT;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.curation.controller.ProductCurationSpecController;
import app.bottlenote.curation.dto.response.CurationSpecListResponse;
import app.bottlenote.curation.dto.response.CurationSpecResponse;
import app.bottlenote.curation.service.CurationSpecQueryService;
import app.docs.AbstractRestDocs;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("restdocs")
@DisplayName("Product 큐레이션 스펙 v2 API 문서화 테스트")
class RestProductCurationSpecControllerTest extends AbstractRestDocs {

  private final CurationSpecQueryService curationSpecQueryService =
      mock(CurationSpecQueryService.class);

  @Override
  protected Object initController() {
    return new ProductCurationSpecController(curationSpecQueryService);
  }

  @Test
  @DisplayName("Product v2 큐레이션 스펙 목록을 조회할 수 있다")
  void getCurationSpecs() throws Exception {
    when(curationSpecQueryService.listActiveSpecs())
        .thenReturn(
            List.of(
                new CurationSpecListResponse(
                    1L, "RECOMMENDED_WHISKY", "추천 위스키", "추천 위스키 카드 목록", 1, true)));

    mockMvc
        .perform(get("/api/v2/curation-specs"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "curation-spec/v2/list",
                responseFields(
                    fieldWithPath("success").type(BOOLEAN).description("응답 성공 여부"),
                    fieldWithPath("code").type(NUMBER).description("응답 코드"),
                    fieldWithPath("data").type(ARRAY).description("활성 큐레이션 스펙 목록"),
                    fieldWithPath("data[].id").type(NUMBER).description("큐레이션 스펙 ID"),
                    fieldWithPath("data[].code").description("큐레이션 스펙 코드"),
                    fieldWithPath("data[].name").description("큐레이션 스펙명"),
                    fieldWithPath("data[].description").description("큐레이션 스펙 설명").optional(),
                    fieldWithPath("data[].version").type(NUMBER).description("스펙 버전"),
                    fieldWithPath("data[].isActive").type(BOOLEAN).description("활성 여부"),
                    fieldWithPath("errors").type(ARRAY).description("에러 목록"),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored())));
  }

  @Test
  @DisplayName("Product v2 큐레이션 스펙 상세를 조회할 수 있다")
  void getCurationSpec() throws Exception {
    when(curationSpecQueryService.getActiveSpecDetail(1L))
        .thenReturn(
            new CurationSpecResponse(
                1L,
                "RECOMMENDED_WHISKY",
                "추천 위스키",
                "추천 위스키 카드 목록",
                "alcohol",
                1,
                true,
                map("type", "object", "required", List.of("source", "alcohol")),
                map("type", "object", "properties", map("stats", map("type", "object")))));

    mockMvc
        .perform(get("/api/v2/curation-specs/{specId}", 1L))
        .andExpect(status().isOk())
        .andDo(
            document(
                "curation-spec/v2/detail",
                pathParameters(parameterWithName("specId").description("큐레이션 스펙 ID")),
                responseFields(
                    fieldWithPath("success").type(BOOLEAN).description("응답 성공 여부"),
                    fieldWithPath("code").type(NUMBER).description("응답 코드"),
                    fieldWithPath("data").type(OBJECT).description("큐레이션 스펙 상세"),
                    fieldWithPath("data.id").type(NUMBER).description("큐레이션 스펙 ID"),
                    fieldWithPath("data.code").description("큐레이션 스펙 코드"),
                    fieldWithPath("data.name").description("큐레이션 스펙명"),
                    fieldWithPath("data.description").description("큐레이션 스펙 설명").optional(),
                    fieldWithPath("data.hydratorKey").description("payload 보강 hydrator key"),
                    fieldWithPath("data.version").type(NUMBER).description("스펙 버전"),
                    fieldWithPath("data.isActive").type(BOOLEAN).description("활성 여부"),
                    subsectionWithPath("data.requestSpec")
                        .type(OBJECT)
                        .description("작성/수정 요청 schema"),
                    subsectionWithPath("data.responseSpec")
                        .type(OBJECT)
                        .description("조회/렌더링 응답 schema"),
                    fieldWithPath("errors").type(ARRAY).description("에러 목록"),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored())));
  }

  private static Map<String, Object> map(Object... values) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put((String) values[i], values[i + 1]);
    }
    return map;
  }
}
