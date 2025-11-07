package app.docs.alcohols;

import static app.bottlenote.alcohols.constant.AlcoholCategoryGroup.SINGLE_MALT;
import static app.bottlenote.review.fixture.ReviewObjectFixture.getReviewListResponse;
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

import app.bottlenote.alcohols.controller.AlcoholQueryController;
import app.bottlenote.alcohols.dto.request.AlcoholSearchRequest;
import app.bottlenote.alcohols.dto.response.AlcoholDetailResponse;
import app.bottlenote.alcohols.dto.response.AlcoholSearchResponse;
import app.bottlenote.alcohols.fixture.AlcoholQueryFixture;
import app.bottlenote.alcohols.service.AlcoholQueryService;
import app.bottlenote.global.service.cursor.PageResponse;
import app.docs.AbstractRestDocs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("alcohol 컨트롤러 RestDocs용 테스트")
class RestAlcoholQueryControllerTest extends AbstractRestDocs {

  private final AlcoholQueryService alcoholQueryService = mock(AlcoholQueryService.class);
  private final AlcoholQueryFixture fixture = new AlcoholQueryFixture();

  @Override
  protected Object initController() {
    return new AlcoholQueryController(alcoholQueryService);
  }

  @DisplayName("술 리스트를 조회할 수 있다.")
  @Test
  void docs_1() throws Exception {
    // given
    PageResponse<AlcoholSearchResponse> response = fixture.getResponse();

    // when
    when(alcoholQueryService.searchAlcohols(any(AlcoholSearchRequest.class), any()))
        .thenReturn(response);

    // then
    mockMvc
        .perform(
            get("/api/v1/alcohols/search")
                .param("keyword", "glen")
                .param("category", String.valueOf(SINGLE_MALT))
                .param("regionId", "1")
                .param("sortType", "REVIEW")
                .param("sortOrder", "DESC")
                .param("cursor", "0")
                .param("pageSize", "3"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "alcohols/search",
                queryParameters(
                    parameterWithName("keyword").optional().description("검색어"),
                    parameterWithName("curationId").optional().description("큐레이션 ID"),
                    parameterWithName("category").optional().description("카테고리 (category API 참조)"),
                    parameterWithName("regionId").optional().description("지역 ID (region API 참조)"),
                    parameterWithName("sortType").optional().description("정렬 타입(해당 문서 하단 enum 참조)"),
                    parameterWithName("sortOrder")
                        .optional()
                        .description("정렬 순서(해당 문서 하단 enum 참조)"),
                    parameterWithName("cursor").optional().description("조회 할 시작 기준 위치"),
                    parameterWithName("pageSize").optional().description("조회 할 페이지 사이즈")),
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드(http status code)"),
                    fieldWithPath("data.totalCount").description("전체 술 리스트의 크기"),
                    fieldWithPath("data.alcohols[].alcoholId").description("술 ID"),
                    fieldWithPath("data.alcohols[].imageUrl").description("술 이미지 URL"),
                    fieldWithPath("data.alcohols[].korName").description("술 한글 이름"),
                    fieldWithPath("data.alcohols[].engName").description("술 영문 이름"),
                    fieldWithPath("data.alcohols[].korCategoryName").description("술 한글 카테고리 이름"),
                    fieldWithPath("data.alcohols[].engCategoryName").description("술 영문 카테고리 이름"),
                    fieldWithPath("data.alcohols[].rating").description("술 평점"),
                    fieldWithPath("data.alcohols[].ratingCount").description("술 평점 개수"),
                    fieldWithPath("data.alcohols[].reviewCount").description("술 리뷰 개수"),
                    fieldWithPath("data.alcohols[].pickCount").description("술 찜 개수"),
                    fieldWithPath("data.alcohols[].isPicked").description("술 찜 여부"),
                    fieldWithPath("errors").ignored(),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored(),
                    fieldWithPath("meta.pageable").description("페이징 정보"),
                    fieldWithPath("meta.pageable.currentCursor").description("조회 시 기준 커서"),
                    fieldWithPath("meta.pageable.cursor").description("다음 페이지 커서"),
                    fieldWithPath("meta.pageable.pageSize").description("조회된 페이지 사이즈"),
                    fieldWithPath("meta.pageable.hasNext").description("다음 페이지 존재 여부"),
                    fieldWithPath("meta.searchParameters.keyword").description("검색 시 사용 한 검색어"),
                    fieldWithPath("meta.searchParameters.curationId")
                        .optional()
                        .description("검색 시 사용 한 큐레이션 ID"),
                    fieldWithPath("meta.searchParameters.category").description("검색 시 사용 한 카테고리"),
                    fieldWithPath("meta.searchParameters.regionId").description("검색 시 사용 한 지역 ID"),
                    fieldWithPath("meta.searchParameters.sortType").description("검색 시 사용 한 정렬 타입"),
                    fieldWithPath("meta.searchParameters.sortOrder").description("검색 시 사용 한 정렬 순서"),
                    fieldWithPath("meta.searchParameters.cursor").description("검색 시 사용 한 커서 기준 "),
                    fieldWithPath("meta.searchParameters.pageSize")
                        .description("검색 시 사용 한 페이지 사이즈"))));
  }

  @DisplayName("술의 상세 정보를 조회 할 수 있다.")
  @Test
  void docs_2() throws Exception {
    AlcoholDetailResponse detail =
        AlcoholDetailResponse.builder()
            .alcohols(fixture.getAlcoholDetailInfo())
            .friendsInfo(fixture.getFriendsDetailInfo())
            .reviewInfo(getReviewListResponse(2))
            .build();

    when(alcoholQueryService.findAlcoholDetailById(any(), any())).thenReturn(detail);

    mockMvc
        .perform(get("/api/v1/alcohols/{alcoholId}", 1))
        .andExpect(status().isOk())
        .andDo(print())
        .andDo(
            document(
                "alcohols/detail",
                responseFields(
                    fieldWithPath("success").description("응답 성공 여부"),
                    fieldWithPath("code").description("응답 코드(http status code)"),

                    // 술 정보
                    fieldWithPath("data.alcohols.alcoholId").description("술 ID"),
                    fieldWithPath("data.alcohols.alcoholUrlImg").description("술 이미지 URL"),
                    fieldWithPath("data.alcohols.korName").description("술의 한국어 이름"),
                    fieldWithPath("data.alcohols.engName").description("술의 영어 이름"),
                    fieldWithPath("data.alcohols.korCategory").description("술의 한국어 카테고리"),
                    fieldWithPath("data.alcohols.engCategory").description("술의 영어 카테고리"),
                    fieldWithPath("data.alcohols.korRegion").description("술의 한국어 지역"),
                    fieldWithPath("data.alcohols.engRegion").description("술의 영어 지역"),
                    fieldWithPath("data.alcohols.cask").description("술의 숙성 캐스크 정보"),
                    fieldWithPath("data.alcohols.abv").description("술의 도수"),
                    fieldWithPath("data.alcohols.korDistillery").description("술 제조사의 한국어 이름"),
                    fieldWithPath("data.alcohols.engDistillery").description("술 제조사의 영어 이름"),
                    fieldWithPath("data.alcohols.rating").description("술의 평균 평점"),
                    fieldWithPath("data.alcohols.totalRatingsCount").description("총 평점 참여자 수"),
                    fieldWithPath("data.alcohols.myRating").description("내가 준 평점"),
                    fieldWithPath("data.alcohols.myAvgRating")
                        .description("내가 지금까지 준 평균 평점(리뷰의 별점을 기반)"),
                    fieldWithPath("data.alcohols.isPicked").description("내가 좋아요한 술인지 여부"),
                    fieldWithPath("data.alcohols.alcoholsTastingTags").description("술의 태그 목록"),

                    // 친구 정보
                    fieldWithPath("data.friendsInfo.followerCount").description("팔로워 수"),
                    fieldWithPath("data.friendsInfo.friends[].userImageUrl")
                        .description("친구의 프로필 이미지 URL"),
                    fieldWithPath("data.friendsInfo.friends[].userId").description("친구의 사용자 ID"),
                    fieldWithPath("data.friendsInfo.friends[].nickName").description("친구의 닉네임"),
                    fieldWithPath("data.friendsInfo.friends[].rating").description("친구의 평점"),

                    // 리뷰 정보
                    fieldWithPath("data.reviewInfo.totalCount").description("해당 술의 총 리뷰 수"),
                    fieldWithPath("data.reviewInfo.reviewList[].reviewId").description("리뷰 ID"),
                    fieldWithPath("data.reviewInfo.reviewList[].reviewContent")
                        .description("리뷰 내용"),
                    fieldWithPath("data.reviewInfo.reviewList[].price").description("리뷰 가격"),
                    fieldWithPath("data.reviewInfo.reviewList[].sizeType")
                        .optional()
                        .description("리뷰 사이즈 타입"),
                    fieldWithPath("data.reviewInfo.reviewList[].likeCount").description("리뷰 좋아요 수"),
                    fieldWithPath("data.reviewInfo.reviewList[].replyCount").description("리뷰 댓글 수"),
                    fieldWithPath("data.reviewInfo.reviewList[].reviewImageUrl")
                        .description("리뷰 이미지 URL"),
                    fieldWithPath("data.reviewInfo.reviewList[].totalImageCount")
                        .description("리뷰 이미지 총개수"),
                    fieldWithPath("data.reviewInfo.reviewList[].userInfo.userId")
                        .description("리뷰 작성자 ID"),
                    fieldWithPath("data.reviewInfo.reviewList[].userInfo.nickName")
                        .description("리뷰 작성자 닉네임"),
                    fieldWithPath("data.reviewInfo.reviewList[].userInfo.userProfileImage")
                        .description("리뷰 작성자 프로필 이미지 URL"),
                    fieldWithPath("data.reviewInfo.reviewList[].rating").description("리뷰 평점"),
                    fieldWithPath("data.reviewInfo.reviewList[].viewCount").description("리뷰 조회수"),
                    fieldWithPath("data.reviewInfo.reviewList[].locationInfo")
                        .description("리뷰 장소 정보"),
                    fieldWithPath("data.reviewInfo.reviewList[].locationInfo.name")
                        .description("리뷰 장소 명"),
                    fieldWithPath("data.reviewInfo.reviewList[].locationInfo.zipCode")
                        .description("리뷰 장소 우편번호"),
                    fieldWithPath("data.reviewInfo.reviewList[].locationInfo.address")
                        .description("리뷰 장소 주소"),
                    fieldWithPath("data.reviewInfo.reviewList[].locationInfo.detailAddress")
                        .description("리뷰 장소 상세 주소"),
                    fieldWithPath("data.reviewInfo.reviewList[].locationInfo.category")
                        .description("리뷰 장소 카테고리"),
                    fieldWithPath("data.reviewInfo.reviewList[].locationInfo.mapUrl")
                        .description("리뷰 장소 지도 URL"),
                    fieldWithPath("data.reviewInfo.reviewList[].locationInfo.latitude")
                        .description("리뷰 장소 위도"),
                    fieldWithPath("data.reviewInfo.reviewList[].locationInfo.longitude")
                        .description("리뷰 장소 경도"),
                    fieldWithPath("data.reviewInfo.reviewList[].status")
                        .description("리뷰 공개 여부 (PUBLIC/PRIVATE)"),
                    fieldWithPath("data.reviewInfo.reviewList[].isMyReview")
                        .description("내가 작성한 리뷰인지 여부"),
                    fieldWithPath("data.reviewInfo.reviewList[].isLikedByMe")
                        .description("내가 좋아요를 눌렀는지 여부"),
                    fieldWithPath("data.reviewInfo.reviewList[].hasReplyByMe")
                        .description("내가 댓글을 달았는지 여부"),
                    fieldWithPath("data.reviewInfo.reviewList[].isBestReview")
                        .description("베스트 리뷰 여부"),
                    fieldWithPath("data.reviewInfo.reviewList[].tastingTagList")
                        .description("리뷰 테이스팅 태그 목록"),
                    fieldWithPath("data.reviewInfo.reviewList[].createAt")
                        .description("리뷰 작성 날짜 'yyyyMMddHHmm' 포맷"),
                    fieldWithPath("errors").ignored(),
                    fieldWithPath("meta.serverVersion").ignored(),
                    fieldWithPath("meta.serverEncoding").ignored(),
                    fieldWithPath("meta.serverResponseTime").ignored(),
                    fieldWithPath("meta.serverPathVersion").ignored())));
  }
}
