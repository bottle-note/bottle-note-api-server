package app.docs.history;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.history.controller.UserHistoryController;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import app.bottlenote.history.fixture.HistoryQueryFixture;
import app.bottlenote.history.service.AlcoholViewHistoryService;
import app.bottlenote.history.service.UserHistoryQueryService;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.shared.cursor.PageResponse;
import app.bottlenote.shared.cursor.SortOrder;
import app.bottlenote.shared.data.response.CollectionResponse;
import app.bottlenote.shared.history.constant.HistoryReviewFilterType;
import app.docs.AbstractRestDocs;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

@DisplayName("UserHistory RestDocs 테스트")
class RestDocsUserHistoryTest extends AbstractRestDocs {

  private final UserHistoryQueryService userHistoryQueryService =
      mock(UserHistoryQueryService.class);
  private final AlcoholViewHistoryService alcoholViewHistoryService =
      mock(AlcoholViewHistoryService.class);
  private final HistoryQueryFixture historyQueryFixture = new HistoryQueryFixture();
  private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

  @Override
  protected Object initController() {
    return new UserHistoryController(userHistoryQueryService, alcoholViewHistoryService);
  }

  @BeforeEach
  void setup() {
    mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
    mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(9L));
  }

  @AfterEach
  void tearDown() {
    mockedSecurityUtil.close();
  }

  @Test
  @DisplayName("유저 히스토리 조회")
  void docs_1() throws Exception {

    // given
    final Long targetUserId = 1L;

    PageResponse<UserHistorySearchResponse> response =
        historyQueryFixture.getUserHistorySearchResponse();

    // when
    when(userHistoryQueryService.findUserHistoryList(any(), any())).thenReturn(response);

    // then
    mockMvc
        .perform(
            get("/api/v1/history/{targetUserId}", targetUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .param("keyword", "글렌피딕")
                .param("ratingPoint", String.valueOf(5))
                .param("historyReviewFilterType", HistoryReviewFilterType.ALL.name())
                .param("picksStatus", PicksStatus.PICK.name())
                .param("startDate", String.valueOf(LocalDateTime.now().minusDays(7)))
                .param("endDate", String.valueOf(LocalDateTime.now()))
                .param("sortOrder", SortOrder.DESC.name())
                .param("cursor", "1")
                .param("pageSize", "3"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "history/search",
                queryParameters(
                    parameterWithName("keyword").description("검색 키워드"),
                    parameterWithName("ratingPoint").description("평점 기준점 (예: 3.0)"),
                    parameterWithName("historyReviewFilterType")
                        .description("필터링 유형 (예: ALL, BEST_REVIEW, REVIEW_LIKE, REVIEW_REPLY)"),
                    parameterWithName("picksStatus").description("픽(pick) 상태 (예: PICK, UNPICK 등)"),
                    parameterWithName("startDate").description("히스토리 조회 시작 일자 (yyyy-MM-dd)"),
                    parameterWithName("endDate").description("히스토리 조회 종료 일자 (yyyy-MM-dd)"),
                    parameterWithName("sortOrder").description("정렬 순서 (ASC 또는 DESC)"),
                    parameterWithName("cursor").description("현재 커서(페이징용)"),
                    parameterWithName("pageSize").description("페이지 사이즈")),
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드(http status code)"),
                    fieldWithPath("errors")
                        .description("응답 성공 여부가 false일 경우 에러 메시지 목록 (없을 경우 빈 배열)"),
                    fieldWithPath("data.totalCount").description("전체 히스토리 개수"),
                    fieldWithPath("data.subscriptionDate").description("(구독 또는 가입 등) 관련 일시"),
                    fieldWithPath("data.userHistories[]").description("히스토리 상세 목록"),
                    fieldWithPath("data.userHistories[].historyId").description("히스토리 ID"),
                    fieldWithPath("data.userHistories[].createdAt")
                        .description("히스토리 생성 시각(yyyy-MM-dd'T'HH:mm:ss)"),
                    fieldWithPath("data.userHistories[].eventCategory").description("이벤트 카테고리"),
                    fieldWithPath("data.userHistories[].eventType").description("이벤트 타입"),
                    fieldWithPath("data.userHistories[].alcoholId")
                        .optional()
                        .description("술 ID (없을 경우 null)"),
                    fieldWithPath("data.userHistories[].alcoholName")
                        .optional()
                        .description("술 이름"),
                    fieldWithPath("data.userHistories[].imageUrl")
                        .optional()
                        .description("이미지 URL"),
                    fieldWithPath("data.userHistories[].redirectUrl")
                        .optional()
                        .description("리다이렉트 URL"),
                    fieldWithPath("data.userHistories[].content")
                        .optional()
                        .description("히스토리 컨텐츠"),
                    fieldWithPath("data.userHistories[].dynamicMessage")
                        .type(JsonFieldType.OBJECT)
                        .optional()
                        .description("동적으로 구성되는 메시지 내용(key-value)"),
                    fieldWithPath("data.userHistories[].dynamicMessage.currentValue")
                        .type(JsonFieldType.STRING)
                        .optional()
                        .description("동적 메시지의 현재 값"),
                    // meta 정보들
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored(),
                    fieldWithPath("meta.pageable").description("페이징 정보"),
                    fieldWithPath("meta.pageable.currentCursor").description("조회 시 기준 커서"),
                    fieldWithPath("meta.pageable.cursor").description("다음 페이지 커서"),
                    fieldWithPath("meta.pageable.pageSize").description("조회된 페이지 사이즈"),
                    fieldWithPath("meta.pageable.hasNext").description("다음 페이지 존재 여부"))));
  }

  @Test
  @DisplayName("유저 알코올 조회 히스토리 조회")
  void docs_2() throws Exception {
    // given
    final Long targetUserId = 9L;
    var items =
        asList(
            historyQueryFixture.getFixtureViewHistoryItem(1L, "글렌피딕"),
            historyQueryFixture.getFixtureViewHistoryItem(2L, "글렌피딕"),
            historyQueryFixture.getFixtureViewHistoryItem(3L, "글렌피딕"));
    var response = CollectionResponse.of(3, items);

    // when
    when(alcoholViewHistoryService.getViewHistory(targetUserId)).thenReturn(response);

    // then
    mockMvc
        .perform(get("/api/v1/history/view/alcohols").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "history/view-alcohols",
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드(http status code)"),
                    fieldWithPath("errors")
                        .description("응답 성공 여부가 false일 경우 에러 메시지 목록 (없을 경우 빈 배열)"),
                    fieldWithPath("data.totalCount").description("전체 조회 알코올 개수"),
                    fieldWithPath("data.items[].alcoholId").description("알코올 ID"),
                    fieldWithPath("data.items[].korName").description("알코올 한글 이름"),
                    fieldWithPath("data.items[].engName").description("알코올 영어 이름"),
                    fieldWithPath("data.items[].rating").description("평점"),
                    fieldWithPath("data.items[].ratingCount").description("평점 개수"),
                    fieldWithPath("data.items[].korCategory").description("카테고리 한글명"),
                    fieldWithPath("data.items[].engCategory").description("카테고리 영문명"),
                    fieldWithPath("data.items[].imageUrl").description("알코올 이미지 URL"),
                    fieldWithPath("data.items[].isPicked").description("사용자 찜 여부"),
                    fieldWithPath("data.items[].popularScore").description("알코올 인기도 점수"),

                    // meta 정보들
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored())));
  }
}
