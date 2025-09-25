package app.docs.review;

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

import app.bottlenote.review.controller.ReviewExploreController;
import app.bottlenote.review.service.ReviewExploreService;
import app.bottlenote.shared.cursor.CursorPageable;
import app.bottlenote.shared.cursor.CursorResponse;
import app.bottlenote.shared.review.dto.response.ReviewExploreItem;
import app.bottlenote.shared.review.payload.UserInfo;
import app.docs.AbstractRestDocs;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("[restdocs] 리뷰 둘러보기 계열 컨트롤러 RestDocs용 테스트")
public class RestReviewExploreControllerTest extends AbstractRestDocs {

  private final ReviewExploreService reviewExploreService = mock(ReviewExploreService.class);

  @Override
  protected Object initController() {
    return new ReviewExploreController(reviewExploreService);
  }

  @DisplayName("리뷰 둘러보기를 할 수 있다.")
  @Test
  void docs_standard_explore() throws Exception {
    // given
    List<String> keywords = List.of("키워드1", "키워드2");
    List<ReviewExploreItem> reviews = createSampleReviewExploreItems();

    CursorPageable pageable =
        CursorPageable.builder().currentCursor(0L).cursor(20L).pageSize(20L).hasNext(true).build();

    CursorResponse<ReviewExploreItem> cursorResponse = CursorResponse.of(reviews, pageable);
    Pair<Long, CursorResponse<ReviewExploreItem>> response = Pair.of(500L, cursorResponse);

    // when
    when(reviewExploreService.getStandardExplore(any(), any(), any(), any())).thenReturn(response);

    // then
    mockMvc
        .perform(
            get("/api/v1/reviews/explore/standard")
                .param("keywords", keywords.get(0))
                .param("keywords", keywords.get(1))
                .param("cursor", "0")
                .param("size", "20"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "reviews/explore/standard",
                queryParameters(
                    parameterWithName("keywords")
                        .optional()
                        .description("검색어 목록 (작성자, 술 이름, 리뷰 내용, 테이스팅 태그에서 검색)"),
                    parameterWithName("cursor").optional().description("조회 할 시작 기준 위치"),
                    parameterWithName("size").optional().description("조회 할 페이지 사이즈")),
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드(http status code)"),
                    fieldWithPath("data.totalCount").description("전체 리뷰 리스트의 크기"),

                    // 사용자 정보 필드 - 수정된 부분
                    fieldWithPath("data.items[].userInfo.userId").description("사용자 ID"),
                    fieldWithPath("data.items[].userInfo.nickName").description("사용자 닉네임"),
                    fieldWithPath("data.items[].userInfo.userProfileImage")
                        .description("사용자 프로필 이미지 URL"),
                    fieldWithPath("data.items[].isMyReview").description("내가 작성한 리뷰 여부"),

                    // 나머지 필드는 동일
                    fieldWithPath("data.items[].alcoholId").description("술 ID"),
                    fieldWithPath("data.items[].alcoholName").description("술 이름"),
                    fieldWithPath("data.items[].reviewId").description("리뷰 ID"),
                    fieldWithPath("data.items[].reviewContent").description("리뷰 내용"),
                    fieldWithPath("data.items[].reviewRating").description("리뷰 평점"),
                    fieldWithPath("data.items[].reviewTags").description("리뷰 테이스팅 태그 목록"),
                    fieldWithPath("data.items[].createAt").description("리뷰 작성 시간"),
                    fieldWithPath("data.items[].modifiedAt").description("리뷰 수정 시간"),
                    fieldWithPath("data.items[].totalImageCount").description("리뷰 이미지 총 개수"),
                    fieldWithPath("data.items[].reviewImages").description("리뷰 이미지 URL 목록"),
                    fieldWithPath("data.items[].isBestReview").description("베스트 리뷰 여부"),
                    fieldWithPath("data.items[].likeCount").description("좋아요 수"),
                    fieldWithPath("data.items[].isLikedByMe").description("내가 좋아요한 리뷰 여부"),
                    fieldWithPath("data.items[].replyCount").description("댓글 수"),
                    fieldWithPath("data.items[].hasReplyByMe").description("내가 댓글을 작성한 리뷰 여부"),

                    // 메타 정보
                    fieldWithPath("errors").ignored(),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored(),
                    fieldWithPath("meta.searchParameters.keywords").description("검색어 정보"),
                    fieldWithPath("meta.pageable").description("페이징 정보"),
                    fieldWithPath("meta.pageable.currentCursor").description("조회 시 기준 커서"),
                    fieldWithPath("meta.pageable.cursor").description("다음 페이지 커서"),
                    fieldWithPath("meta.pageable.pageSize").description("조회된 페이지 사이즈"),
                    fieldWithPath("meta.pageable.hasNext").description("다음 페이지 존재 여부"))));
  }

  /** 테스트용 샘플 리뷰 데이터 생성 */
  private List<ReviewExploreItem> createSampleReviewExploreItems() {
    List<ReviewExploreItem> items = new ArrayList<>();

    // 샘플 리뷰 3개 생성
    for (int i = 1; i <= 3; i++) {
      UserInfo userInfo =
          UserInfo.of((long) i, "사용자" + i, "https://example.com/profile" + i + ".jpg");

      List<String> tags = List.of("달달한", "과일향", "오크향");
      List<String> images =
          List.of(
              "https://example.com/image" + i + "_1.jpg",
              "https://example.com/image" + i + "_2.jpg");

      ReviewExploreItem item =
          new ReviewExploreItem(
              userInfo,
              i == 1, // 첫 번째 리뷰만 내 리뷰로 설정
              (long) (100 + i), // alcoholId
              "시그넷 위스키 " + i, // alcoholName
              (long) (1000 + i), // reviewId
              "이 위스키는 정말 맛있습니다. 과일향과 오크향이 절묘하게 어우러져 있어요. " + i, // reviewContent
              4.5 + (i * 0.1), // reviewRating
              tags, // reviewTags
              LocalDateTime.now().minusDays(i), // createAt
              LocalDateTime.now().minusHours(i), // modifiedAt
              (long) (images.size()), // totalImageCount
              images, // reviewImages
              i == 2, // 두 번째 리뷰만 베스트 리뷰로 설정
              (long) (10 * i), // likeCount
              i == 3, // 세 번째 리뷰만 좋아요 표시
              (long) (5 * i), // replyCount
              i == 1 // 첫 번째 리뷰만 댓글 작성
              );

      items.add(item);
    }

    return items;
  }
}
