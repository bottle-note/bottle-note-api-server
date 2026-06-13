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

import app.bottlenote.curation.controller.ProductSpecBasedCurationController;
import app.bottlenote.curation.dto.response.ProductSpecBasedCurationDetailResponse;
import app.bottlenote.curation.dto.response.ProductSpecBasedCurationFeedItemResponse;
import app.bottlenote.curation.dto.response.ProductSpecBasedCurationListResponse;
import app.bottlenote.curation.service.ProductSpecBasedCurationService;
import app.bottlenote.global.service.cursor.CursorResponse;
import app.docs.AbstractRestDocs;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("restdocs")
@DisplayName("Product spec 기반 큐레이션 v2 API 문서화 테스트")
class RestProductSpecBasedCurationControllerTest extends AbstractRestDocs {

  private final ProductSpecBasedCurationService productSpecBasedCurationService =
      mock(ProductSpecBasedCurationService.class);

  @Override
  protected Object initController() {
    return new ProductSpecBasedCurationController(productSpecBasedCurationService);
  }

  @Test
  @DisplayName("spec 기반 큐레이션 v2 목록을 조회할 수 있다")
  void getCurations() throws Exception {
    when(productSpecBasedCurationService.listActiveCurations())
        .thenReturn(
            List.of(
                new ProductSpecBasedCurationListResponse(
                    1L,
                    1L,
                    "RECOMMENDED_WHISKY",
                    "추천 위스키",
                    "비 오는 날 위스키",
                    "스모키 위스키 추천",
                    "https://cdn.example.com/cover.jpg",
                    List.of(
                        "https://cdn.example.com/cover.jpg", "https://cdn.example.com/second.jpg"),
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30),
                    1,
                    LocalDateTime.of(2026, 5, 15, 12, 0))));

    mockMvc
        .perform(get("/api/v2/curations"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "curation/v2/list",
                responseFields(
                    fieldWithPath("success").type(BOOLEAN).description("응답 성공 여부"),
                    fieldWithPath("code").type(NUMBER).description("응답 코드"),
                    fieldWithPath("data").type(ARRAY).description("spec 기반 큐레이션 목록"),
                    fieldWithPath("data[].id").type(NUMBER).description("큐레이션 ID"),
                    fieldWithPath("data[].specId").type(NUMBER).description("큐레이션 스펙 ID"),
                    fieldWithPath("data[].specCode").description("큐레이션 스펙 코드"),
                    fieldWithPath("data[].specName").description("큐레이션 스펙명"),
                    fieldWithPath("data[].name").description("큐레이션 이름"),
                    fieldWithPath("data[].description").description("큐레이션 설명").optional(),
                    fieldWithPath("data[].coverImageUrl").description("대표 이미지 URL"),
                    fieldWithPath("data[].imageUrls").type(ARRAY).description("큐레이션 이미지 URL 목록"),
                    fieldWithPath("data[].exposureStartDate")
                        .type(ARRAY)
                        .description("노출 시작일")
                        .optional(),
                    fieldWithPath("data[].exposureEndDate")
                        .type(ARRAY)
                        .description("노출 종료일")
                        .optional(),
                    fieldWithPath("data[].displayOrder").type(NUMBER).description("노출 순서"),
                    fieldWithPath("data[].createAt").type(ARRAY).description("생성 일시"),
                    fieldWithPath("errors").type(ARRAY).description("에러 목록"),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored())));
  }

  @Test
  @DisplayName("spec 기반 큐레이션 v2 피드를 상세 응답 기반 payload로 조회할 수 있다")
  void getCurationFeed() throws Exception {
    when(productSpecBasedCurationService.searchFeed(0L, 10))
        .thenReturn(
            CursorResponse.of(
                List.of(
                    new ProductSpecBasedCurationFeedItemResponse(
                        1L,
                        "비 오는 날 위스키",
                        "스모키 위스키 추천",
                        "https://cdn.example.com/cover.jpg",
                        List.of("https://cdn.example.com/cover.jpg"),
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30),
                        1,
                        LocalDateTime.of(2026, 5, 15, 12, 0),
                        List.of(
                            map(
                                "alcohol",
                                map(
                                    "alcoholId",
                                    1,
                                    "korName",
                                    "테스트 위스키",
                                    "selectedTags",
                                    List.of("셰리")),
                                "comment",
                                "추천 코멘트")))),
                0L,
                10));

    mockMvc
        .perform(get("/api/v2/curations/feed"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "curation/v2/feed",
                responseFields(
                    fieldWithPath("success").type(BOOLEAN).description("응답 성공 여부"),
                    fieldWithPath("code").type(NUMBER).description("응답 코드"),
                    fieldWithPath("data.items").type(ARRAY).description("큐레이션 피드 목록"),
                    fieldWithPath("data.items[].id").type(NUMBER).description("큐레이션 ID"),
                    fieldWithPath("data.items[].name").description("큐레이션 이름"),
                    fieldWithPath("data.items[].description").description("큐레이션 설명").optional(),
                    fieldWithPath("data.items[].coverImageUrl").description("대표 이미지 URL"),
                    fieldWithPath("data.items[].imageUrls")
                        .type(ARRAY)
                        .description("큐레이션 이미지 URL 목록"),
                    fieldWithPath("data.items[].exposureStartDate")
                        .type(ARRAY)
                        .description("노출 시작일")
                        .optional(),
                    fieldWithPath("data.items[].exposureEndDate")
                        .type(ARRAY)
                        .description("노출 종료일")
                        .optional(),
                    fieldWithPath("data.items[].displayOrder").type(NUMBER).description("노출 순서"),
                    fieldWithPath("data.items[].createAt").type(ARRAY).description("생성 일시"),
                    subsectionWithPath("data.items[].payload")
                        .type(ARRAY)
                        .description("상세 payload와 동일한 구조에서 x-feed enabled 필드만 남긴 payload"),
                    fieldWithPath("data.pageable.currentCursor").type(NUMBER).description("현재 커서"),
                    fieldWithPath("data.pageable.cursor").type(NUMBER).description("다음 커서"),
                    fieldWithPath("data.pageable.pageSize").type(NUMBER).description("페이지 크기"),
                    fieldWithPath("data.pageable.hasNext").type(BOOLEAN).description("다음 페이지 여부"),
                    fieldWithPath("errors").type(ARRAY).description("에러 목록"),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored())));
  }

  @Test
  @DisplayName("spec 기반 큐레이션 v2 상세를 materialized payload로 조회할 수 있다")
  void getCuration() throws Exception {
    when(productSpecBasedCurationService.getDetail(1L))
        .thenReturn(
            new ProductSpecBasedCurationDetailResponse(
                1L,
                "비 오는 날 위스키",
                "스모키 위스키 추천",
                "https://cdn.example.com/cover.jpg",
                List.of("https://cdn.example.com/cover.jpg"),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                1,
                LocalDateTime.of(2026, 5, 15, 12, 0),
                new ProductSpecBasedCurationDetailResponse.SpecMeta(
                    1L,
                    "RECOMMENDED_WHISKY",
                    "추천 위스키",
                    "array",
                    map("type", "object", "x-container", "array")),
                List.of(
                    item(
                        "BOTTLE_NOTE",
                        map("alcoholId", 1, "korName", "테스트 위스키", "selectedTags", List.of("셰리")),
                        map(
                            "rating",
                            4.2,
                            "totalRatingsCount",
                            10,
                            "reviewCount",
                            3,
                            "totalPickCount",
                            8)),
                    item(
                        "MANUAL",
                        map("alcoholId", null, "korName", "수동 위스키", "selectedTags", List.of("오크")),
                        null))));

    mockMvc
        .perform(get("/api/v2/curations/{curationId}", 1L))
        .andExpect(status().isOk())
        .andDo(
            document(
                "curation/v2/detail",
                pathParameters(parameterWithName("curationId").description("큐레이션 ID")),
                responseFields(
                    fieldWithPath("success").type(BOOLEAN).description("응답 성공 여부"),
                    fieldWithPath("code").type(NUMBER).description("응답 코드"),
                    fieldWithPath("data").type(OBJECT).description("큐레이션 상세"),
                    fieldWithPath("data.id").type(NUMBER).description("큐레이션 ID"),
                    fieldWithPath("data.name").description("큐레이션 이름"),
                    fieldWithPath("data.description").description("큐레이션 설명").optional(),
                    fieldWithPath("data.coverImageUrl").description("대표 이미지 URL"),
                    fieldWithPath("data.imageUrls").type(ARRAY).description("큐레이션 이미지 URL 목록"),
                    fieldWithPath("data.exposureStartDate")
                        .type(ARRAY)
                        .description("노출 시작일")
                        .optional(),
                    fieldWithPath("data.exposureEndDate")
                        .type(ARRAY)
                        .description("노출 종료일")
                        .optional(),
                    fieldWithPath("data.displayOrder").type(NUMBER).description("노출 순서"),
                    fieldWithPath("data.createAt").type(ARRAY).description("생성 일시"),
                    fieldWithPath("data.spec").type(OBJECT).description("스펙 메타"),
                    fieldWithPath("data.spec.id").type(NUMBER).description("스펙 ID"),
                    fieldWithPath("data.spec.code").description("스펙 코드"),
                    fieldWithPath("data.spec.name").description("스펙명"),
                    fieldWithPath("data.spec.container")
                        .description("payload 컨테이너 타입(array 또는 object)"),
                    subsectionWithPath("data.spec.responseSpec")
                        .type(OBJECT)
                        .description("Product 응답 검증 기준 OpenAPI response schema"),
                    subsectionWithPath("data.payload")
                        .type(ARRAY)
                        .description("responseSpec 기준으로 materialized 된 payload"),
                    fieldWithPath("errors").type(ARRAY).description("에러 목록"),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored())));
  }

  private static Map<String, Object> item(
      String source, Map<String, Object> alcohol, Map<String, Object> stats) {
    return map("source", source, "alcohol", alcohol, "comment", "추천 코멘트", "stats", stats);
  }

  private static Map<String, Object> map(Object... values) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      map.put((String) values[i], values[i + 1]);
    }
    return map;
  }
}
