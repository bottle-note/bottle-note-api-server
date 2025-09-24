package app.docs.alcohols;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.alcohols.domain.controller.AlcoholReferenceController;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.dto.response.RegionsItem;
import app.bottlenote.alcohols.fixture.AlcoholQueryFixture;
import app.bottlenote.alcohols.service.AlcoholReferenceService;
import app.docs.AbstractRestDocs;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@DisplayName("알코올 참조 컨트롤러 RestDocs용 테스트")
class RestReferenceControllerTest extends AbstractRestDocs {

  private final AlcoholQueryFixture fixture = new AlcoholQueryFixture();
  private final AlcoholReferenceService referenceService = mock(AlcoholReferenceService.class);

  @Override
  protected Object initController() {
    return new AlcoholReferenceController(referenceService);
  }

  @Test
  @DisplayName("지역 리스트를 조회할 수 있다.")
  void docs_1() throws Exception {
    // given
    List<RegionsItem> response =
        List.of(
            RegionsItem.of(1L, "스코틀랜드/로우랜드", "Scotland/Lowlands", "가벼운 맛이 특징인 로우랜드 위스키"),
            RegionsItem.of(
                2L,
                "스코틀랜드/하이랜드",
                "Scotland/Highlands",
                "맛의 다양성이 특징인 하이랜드 위스키, 해안의 짠맛부터 달콤하고 과일 맛까지"),
            RegionsItem.of(3L, "스코틀랜드/아일랜드", "Scotland/Ireland", "부드러운 맛이 특징인 아일랜드 위스키"),
            RegionsItem.of(11L, "프랑스", "France", "주로 브랜디와 와인 생산지로 유명하지만 위스키도 생산"),
            RegionsItem.of(12L, "스웨덴", "Sweden", "실험적인 방법으로 만드는 스웨덴 위스키"));
    // when
    when(referenceService.findAllRegion()).thenReturn(response);

    // then
    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/v1/regions/"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "alcohols/regions",
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드(http status code)"),
                    fieldWithPath("data[].regionId").description("지역 ID"),
                    fieldWithPath("data[].korName").description("지역 한글명"),
                    fieldWithPath("data[].engName").description("지역 이름"),
                    fieldWithPath("data[].description").description("지역 설명"),
                    fieldWithPath("errors")
                        .type(JsonFieldType.ARRAY)
                        .description("응답 성공 여부가 false일 경우 에러 메시지(없을 경우 null)"),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored())));
  }

  @DisplayName("카테고리 정보를 조회 할 수 있다.")
  @Test
  void docs_2() throws Exception {
    List<CategoryItem> responses = fixture.categoryResponses();

    when(referenceService.getAlcoholCategory(any())).thenReturn(responses);

    mockMvc
        .perform(get("/api/v1/alcohols/categories").param("type", "WHISKY"))
        .andExpect(status().isOk())
        .andDo(print())
        .andDo(
            document(
                "alcohols/categories",
                queryParameters(
                    parameterWithName("type").description("카테고리 타입 (해당 문서 하단 enum 참조)")),
                responseFields(
                    fieldWithPath("success").ignored(),
                    fieldWithPath("code").ignored(),
                    fieldWithPath("data[].korCategory").description("카테고리 한글 이름"),
                    fieldWithPath("data[].engCategory").description("카테고리 영어 이름"),
                    fieldWithPath("data[].categoryGroup")
                        .description("카테고리 그룹 (카테고리 검색 조건 사용 시 사용) "),
                    fieldWithPath("errors").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored())));
  }
}
