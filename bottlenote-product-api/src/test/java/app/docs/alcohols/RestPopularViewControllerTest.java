package app.docs.alcohols;

import static app.bottlenote.alcohols.fixture.PopularsObjectFixture.getFixturePopulars;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.alcohols.controller.AlcoholPopularQueryController;
import app.bottlenote.alcohols.dto.response.PopularItem;
import app.bottlenote.alcohols.service.AlcoholPopularService;
import app.docs.AbstractRestDocs;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@DisplayName("조회수 기반 인기 위스키 RestDocs 테스트")
class RestPopularViewControllerTest extends AbstractRestDocs {

  private final AlcoholPopularService alcoholPopularService = mock(AlcoholPopularService.class);

  @Override
  protected Object initController() {
    return new AlcoholPopularQueryController(alcoholPopularService);
  }

  @Test
  @DisplayName("주간 조회수 기반 인기 위스키를 조회할 수 있다")
  void docs_getPopularViewWeekly() throws Exception {
    // given
    List<PopularItem> populars =
        List.of(
            getFixturePopulars(1L, "글렌피딕", "Glenfiddich"),
            getFixturePopulars(2L, "맥캘란", "Macallan"),
            getFixturePopulars(3L, "글렌리벳", "Glenlivet"),
            getFixturePopulars(4L, "발베니", "Balvenie"),
            getFixturePopulars(5L, "라프로익", "Laphroaig"));

    when(alcoholPopularService.getPopularByViewsWeekly(anyInt(), any())).thenReturn(populars);

    // when & then
    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/v1/popular/view/week").param("top", "5"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "alcohols/populars/view/week",
                queryParameters(parameterWithName("top").description("조회할 위스키 개수 (기본값: 20)")),
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드(http status code)"),
                    fieldWithPath("data.totalCount").description("조회된 위스키 개수"),
                    fieldWithPath("data.alcohols").description("주간 조회수 기반 인기 위스키 리스트"),
                    fieldWithPath("data.alcohols[].alcoholId").description("위스키 ID"),
                    fieldWithPath("data.alcohols[].korName").description("위스키 한글명"),
                    fieldWithPath("data.alcohols[].engName").description("위스키 영문명"),
                    fieldWithPath("data.alcohols[].rating").description("평균 평점"),
                    fieldWithPath("data.alcohols[].ratingCount").description("평점 참여자 수"),
                    fieldWithPath("data.alcohols[].korCategory").description("카테고리 한글명"),
                    fieldWithPath("data.alcohols[].engCategory").description("카테고리 영문명"),
                    fieldWithPath("data.alcohols[].imageUrl").description("이미지 URL"),
                    fieldWithPath("data.alcohols[].isPicked").description("찜 여부"),
                    fieldWithPath("data.alcohols[].popularScore").description("인기도 점수 (조회수 기반)"),
                    fieldWithPath("errors").ignored(),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored())));

    verify(alcoholPopularService).getPopularByViewsWeekly(5, -1L);
  }

  @Test
  @DisplayName("월간 조회수 기반 인기 위스키를 조회할 수 있다")
  void docs_getPopularViewMonthly() throws Exception {
    // given
    List<PopularItem> populars =
        List.of(
            getFixturePopulars(1L, "글렌피딕", "Glenfiddich"),
            getFixturePopulars(2L, "맥캘란", "Macallan"),
            getFixturePopulars(3L, "글렌리벳", "Glenlivet"),
            getFixturePopulars(4L, "발베니", "Balvenie"),
            getFixturePopulars(5L, "라프로익", "Laphroaig"));

    when(alcoholPopularService.getPopularByViewsMonthly(anyInt(), any())).thenReturn(populars);

    // when & then
    mockMvc
        .perform(MockMvcRequestBuilders.get("/api/v1/popular/view/monthly").param("top", "5"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "alcohols/populars/view/monthly",
                queryParameters(parameterWithName("top").description("조회할 위스키 개수 (기본값: 20)")),
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드(http status code)"),
                    fieldWithPath("data.totalCount").description("조회된 위스키 개수"),
                    fieldWithPath("data.alcohols").description("월간 조회수 기반 인기 위스키 리스트"),
                    fieldWithPath("data.alcohols[].alcoholId").description("위스키 ID"),
                    fieldWithPath("data.alcohols[].korName").description("위스키 한글명"),
                    fieldWithPath("data.alcohols[].engName").description("위스키 영문명"),
                    fieldWithPath("data.alcohols[].rating").description("평균 평점"),
                    fieldWithPath("data.alcohols[].ratingCount").description("평점 참여자 수"),
                    fieldWithPath("data.alcohols[].korCategory").description("카테고리 한글명"),
                    fieldWithPath("data.alcohols[].engCategory").description("카테고리 영문명"),
                    fieldWithPath("data.alcohols[].imageUrl").description("이미지 URL"),
                    fieldWithPath("data.alcohols[].isPicked").description("찜 여부"),
                    fieldWithPath("data.alcohols[].popularScore").description("인기도 점수 (조회수 기반)"),
                    fieldWithPath("errors").ignored(),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored())));

    verify(alcoholPopularService).getPopularByViewsMonthly(5, -1L);
  }
}
