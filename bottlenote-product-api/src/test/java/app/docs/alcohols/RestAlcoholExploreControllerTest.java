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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.alcohols.controller.AlcoholExploreController;
import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
import app.bottlenote.alcohols.fixture.AlcoholQueryFixture;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.CursorResponse;
import app.docs.AbstractRestDocs;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("alcohol 컨트롤러 RestDocs용 테스트")
public class RestAlcoholExploreControllerTest extends AbstractRestDocs {
  private final AlcoholQueryService alcoholQueryService = mock(AlcoholQueryService.class);

  @Override
  protected Object initController() {
    return new AlcoholExploreController(alcoholQueryService);
  }

  @DisplayName("위스키 둘러보기를 할 수 있다.")
  @Test
  void docs_1() throws Exception {
    // given
    List<String> keywords = List.of("keyword1", "keyword2");
    List<AlcoholDetailItem> alcohols =
        List.of(
            AlcoholQueryFixture.getAlcoholDetailInfo(),
            AlcoholQueryFixture.getAlcoholDetailInfo(),
            AlcoholQueryFixture.getAlcoholDetailInfo());
    CursorPageable pageable =
        CursorPageable.builder().currentCursor(0L).cursor(0L).pageSize(10L).hasNext(true).build();
    CursorResponse<AlcoholDetailItem> cursorResponse = CursorResponse.of(alcohols, pageable);

    // when
    when(alcoholQueryService.getStandardExplore(any(), any())).thenReturn(cursorResponse);

    // then
    mockMvc
        .perform(
            get("/api/v1/alcohols/explore/standard")
                .param("keywords", keywords.get(0))
                .param("keywords", keywords.get(1))
                .param("cursor", "0")
                .param("size", "10"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "alcohols/explore/standard",
                queryParameters(
                    parameterWithName("keywords")
                        .optional()
                        .description("검색어 목록 (다중 파라미터, 단어 간 AND 결합)"),
                    parameterWithName("category")
                        .optional()
                        .description("카테고리 필터 (AlcoholCategoryGroup)"),
                    parameterWithName("regionIds")
                        .optional()
                        .description("지역 ID 목록 (OR, 부모 지역이면 자식 포함)"),
                    parameterWithName("distilleryIds").optional().description("증류소 ID 목록 (OR)"),
                    parameterWithName("curationId").optional().description("큐레이션 ID"),
                    parameterWithName("sortType")
                        .optional()
                        .description("정렬 타입 (RANDOM/POPULAR/RATING/REVIEW/PICK, 기본 RANDOM)"),
                    parameterWithName("sortOrder")
                        .optional()
                        .description("정렬 순서 (ASC/DESC, 기본 DESC)"),
                    parameterWithName("cursor").optional().description("조회 할 시작 기준 위치"),
                    parameterWithName("size").optional().description("조회 할 페이지 사이즈")),
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드(http status code)"),
                    fieldWithPath("data.totalCount").description("전체 술 리스트의 크기"),
                    fieldWithPath("data.items[].alcoholId").description("술 ID"),
                    fieldWithPath("data.items[].alcoholUrlImg").description("술 이미지 URL"),
                    fieldWithPath("data.items[].korName").description("술 한글 이름"),
                    fieldWithPath("data.items[].engName").description("술 영문 이름"),
                    fieldWithPath("data.items[].korCategory").description("술 한글 카테고리"),
                    fieldWithPath("data.items[].engCategory").description("술 영문 카테고리"),
                    fieldWithPath("data.items[].korRegion").description("술 한글 지역"),
                    fieldWithPath("data.items[].engRegion").description("술 영문 지역"),
                    fieldWithPath("data.items[].cask").description("캐스크 정보"),
                    fieldWithPath("data.items[].abv").description("알코올 도수"),
                    fieldWithPath("data.items[].korDistillery").description("한글 증류소 이름"),
                    fieldWithPath("data.items[].engDistillery").description("영문 증류소 이름"),
                    fieldWithPath("data.items[].rating").description("술 평점"),
                    fieldWithPath("data.items[].totalRatingsCount").description("술 평점 총 개수"),
                    fieldWithPath("data.items[].myRating").description("내 평점"),
                    fieldWithPath("data.items[].myAvgRating").description("내 평균 평점"),
                    fieldWithPath("data.items[].isPicked").description("술 찜 여부"),
                    fieldWithPath("data.items[].reviewCount").description("리뷰 수"),
                    fieldWithPath("data.items[].pickCount").description("찜 수"),
                    fieldWithPath("data.items[].alcoholsTastingTags").description("테이스팅 태그 목록"),
                    fieldWithPath("errors").ignored(),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored(),
                    fieldWithPath("meta.searchParameters.keywords").description("검색어 정보"),
                    fieldWithPath("meta.searchParameters.category")
                        .optional()
                        .description("적용된 카테고리 필터"),
                    fieldWithPath("meta.searchParameters.regionIds").description("적용된 지역 ID 목록"),
                    fieldWithPath("meta.searchParameters.distilleryIds")
                        .description("적용된 증류소 ID 목록"),
                    fieldWithPath("meta.searchParameters.curationId")
                        .optional()
                        .description("적용된 큐레이션 ID"),
                    fieldWithPath("meta.searchParameters.sortType").description("적용된 정렬 타입"),
                    fieldWithPath("meta.searchParameters.sortOrder").description("적용된 정렬 순서"),
                    fieldWithPath("meta.searchParameters.cursor").description("요청 커서"),
                    fieldWithPath("meta.searchParameters.size").description("요청 사이즈"),
                    fieldWithPath("meta.pageable").description("페이징 정보"),
                    fieldWithPath("meta.pageable.currentCursor").description("조회 시 기준 커서"),
                    fieldWithPath("meta.pageable.cursor").description("다음 페이지 커서"),
                    fieldWithPath("meta.pageable.pageSize").description("조회된 페이지 사이즈"),
                    fieldWithPath("meta.pageable.hasNext").description("다음 페이지 존재 여부"))));
  }
}
