package app.bottlenote.docs.user;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.controller.UserMyPageController;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.fixture.UserQueryFixture;
import app.bottlenote.user.service.UserBasicService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("rest-docs")
@DisplayName("[restdocs] 마이페이지 컨트롤러 RestDocs용 테스트")
class RestDocsUserQueryControllerTest extends AbstractRestDocs {

	private final UserBasicService userQueryService = mock(UserBasicService.class);
	private final UserQueryFixture mypageQueryFixture = new UserQueryFixture();
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	@Override
	protected Object initController() {
		return new UserMyPageController(userQueryService);
	}

	@BeforeEach
	void setup() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
		mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(1L));
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@Test
	@DisplayName("마이페이지 유저 정보를 조회할 수 있다.")
	void test_1() throws Exception {
		// given
		Long userId = 1L;
		MyPageResponse myPageUserInfo = mypageQueryFixture.getMyPageInfo();

		// when
		Mockito.when(userQueryService.getMyPage(any(), any())).thenReturn(myPageUserInfo);

		// then
		mockMvc.perform(get("/api/v1/my-page/{userId}", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.with(csrf()))
				.andExpect(status().isOk())
				.andDo(document("user/mypage",
						pathParameters(
								parameterWithName("userId").description("유저 아이디")
						),
						responseFields(
								fieldWithPath("success").description("응답 성공 여부"),
								fieldWithPath("code").description("응답 코드"),
								fieldWithPath("data").description("응답 데이터"),
								fieldWithPath("data.userId").description("유저 아이디"),
								fieldWithPath("data.nickName").description("유저 닉네임"),
								fieldWithPath("data.imageUrl").description("유저 프로필 이미지 URL"),
								fieldWithPath("data.reviewCount").description("리뷰 수"),
								fieldWithPath("data.ratingCount").description("평점 수"),
								fieldWithPath("data.pickCount").description("찜한 수"),
								fieldWithPath("data.followerCount").description("팔로워 수"),
								fieldWithPath("data.followingCount").description("팔로잉 수"),
								fieldWithPath("data.isFollow").description("팔로우 여부"),
								fieldWithPath("data.isMyPage").description("본인 여부"),
								fieldWithPath("errors").ignored(),
								fieldWithPath("meta.serverVersion").ignored(),
								fieldWithPath("meta.serverEncoding").ignored(),
								fieldWithPath("meta.serverResponseTime").ignored(),
								fieldWithPath("meta.serverPathVersion").ignored()
						)
				));
	}

	@Test
	@DisplayName("마이 보틀(리뷰) 정보를 조회할 수 있다.")
	void test_2() throws Exception {
		// given
		Long userId = 8L;
		MyBottleResponse myBottleResponse = mypageQueryFixture.getReviewMyBottleResponse(userId, true, null);

		// when
		Mockito.when(userQueryService.getMyBottle(any(), any(), any(), any())).thenReturn(myBottleResponse);

		// then
		mockMvc.perform(get("/api/v1/my-page/{userId}/my-bottle/review", userId)
						.param("keyword", "")
						.param("regionId", "")
						.param("sortType", "LATEST")
						.param("sortOrder", "DESC")
						.param("cursor", "0")
						.param("pageSize", "50")
						.contentType(MediaType.APPLICATION_JSON)
						.with(csrf()))
				.andExpect(status().isOk())
				.andDo(document("user/mybottle",
						pathParameters(
								parameterWithName("userId").description("유저 아이디")
						),
						queryParameters(
								parameterWithName("keyword").optional().description("검색 키워드"),
								parameterWithName("regionId").optional().description("지역 ID"),
								parameterWithName("sortType").optional().description("정렬 타입(해당 문서 하단 enum 참조)"),
								parameterWithName("sortOrder").optional().description("정렬 순서(해당 문서 하단 enum 참조)"),
								parameterWithName("cursor").optional().description("조회 할 시작 기준 위치"),
								parameterWithName("pageSize").optional().description("조회 할 페이지 사이즈"),
								parameterWithName("_csrf").ignored()
						),
						responseFields(
								fieldWithPath("success").description("응답 성공 여부"),
								fieldWithPath("code").description("응답 코드"),
								fieldWithPath("data").description("응답 데이터"),
								fieldWithPath("data.userId").description("유저 아이디"),
								fieldWithPath("data.isMyPage").description("본인 여부"),
								fieldWithPath("data.totalCount").description("전체 보틀 수"),
								fieldWithPath("data.myBottleList").description("보틀 목록"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.alcoholId").description("알코올 아이디"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.alcoholKorName").description("알코올 한글명"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.alcoholEngName").description("알코올 영문명"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.korCategoryName").description("알코올 카테고리명"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.imageUrl").description("알코올 이미지 URL"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.isHot5").description("HOT5 여부"),
								fieldWithPath("data.myBottleList[].reviewId").description("리뷰 ID").optional(),
								fieldWithPath("data.myBottleList[].isMyReview").description("내가 작성한 리뷰 여부"),
								fieldWithPath("data.myBottleList[].reviewModifyAt").description("리뷰 마지막 수정일").optional(),
								fieldWithPath("data.myBottleList[].reviewContent").description("리뷰 내용").optional(),
								fieldWithPath("data.myBottleList[].reviewTastingTags").description("리뷰 테이스팅 태그").optional(),
								fieldWithPath("data.myBottleList[].isBestReview").description("베스트 리뷰 여부"),
								fieldWithPath("data.cursorPageable").description("커서 페이지 정보").optional(),
								fieldWithPath("errors").ignored(),
								fieldWithPath("meta.serverVersion").ignored(),
								fieldWithPath("meta.serverEncoding").ignored(),
								fieldWithPath("meta.serverResponseTime").ignored(),
								fieldWithPath("meta.serverPathVersion").ignored()
						)
				));
	}

	@Test
	@DisplayName("마이 보틀(별점) 정보를 조회할 수 있다.")
	void test_3() throws Exception {
		// given
		Long userId = 8L;
		MyBottleResponse myBottleResponse = mypageQueryFixture.getRatingMyBottleResponse(userId, true, null);

		// when
		Mockito.when(userQueryService.getMyBottle(any(), any(), any(), any())).thenReturn(myBottleResponse);

		// then
		mockMvc.perform(get("/api/v1/my-page/{userId}/my-bottle/rating", userId)
						.param("keyword", "")
						.param("regionId", "")
						.param("sortType", "LATEST")
						.param("sortOrder", "DESC")
						.param("cursor", "0")
						.param("pageSize", "50")
						.contentType(MediaType.APPLICATION_JSON)
						.with(csrf()))
				.andExpect(status().isOk())
				.andDo(document("user/mybottle",
						pathParameters(
								parameterWithName("userId").description("유저 아이디")
						),
						queryParameters(
								parameterWithName("keyword").optional().description("검색 키워드"),
								parameterWithName("regionId").optional().description("지역 ID"),
								parameterWithName("sortType").optional().description("정렬 타입(해당 문서 하단 enum 참조)"),
								parameterWithName("sortOrder").optional().description("정렬 순서(해당 문서 하단 enum 참조)"),
								parameterWithName("cursor").optional().description("조회 할 시작 기준 위치"),
								parameterWithName("pageSize").optional().description("조회 할 페이지 사이즈"),
								parameterWithName("_csrf").ignored()
						),
						responseFields(
								fieldWithPath("success").description("응답 성공 여부"),
								fieldWithPath("code").description("응답 코드"),
								fieldWithPath("data").description("응답 데이터"),
								fieldWithPath("data.userId").description("유저 아이디"),
								fieldWithPath("data.isMyPage").description("본인 여부"),
								fieldWithPath("data.totalCount").description("전체 보틀 수"),
								fieldWithPath("data.myBottleList").description("보틀 목록"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.alcoholId").description("알코올 아이디"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.alcoholKorName").description("알코올 한글명"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.alcoholEngName").description("알코올 영문명"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.korCategoryName").description("알코올 카테고리명"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.imageUrl").description("알코올 이미지 URL"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.isHot5").description("HOT5 여부"),
								fieldWithPath("data.myBottleList[].myRatingPoint").description("내 별점").optional(),
								fieldWithPath("data.myBottleList[].averageRatingPoint").description("전체 평균 별점"),
								fieldWithPath("data.myBottleList[].averageRatingCount").description("전체 평균 별점 참여자 수").optional(),
								fieldWithPath("data.myBottleList[].ratingModifyAt").description("별점 수정시간").optional(),
								fieldWithPath("data.cursorPageable").description("커서 페이지 정보").optional(),
								fieldWithPath("errors").ignored(),
								fieldWithPath("meta.serverVersion").ignored(),
								fieldWithPath("meta.serverEncoding").ignored(),
								fieldWithPath("meta.serverResponseTime").ignored(),
								fieldWithPath("meta.serverPathVersion").ignored()
						)
				));
	}

	@Test
	@DisplayName("마이 보틀(찜) 정보를 조회할 수 있다.")
	void test_4() throws Exception {
		// given
		Long userId = 8L;
		MyBottleResponse myBottleResponse = mypageQueryFixture.getPicksMyBottleResponse(userId, true, null);

		// when
		Mockito.when(userQueryService.getMyBottle(any(), any(), any(), any())).thenReturn(myBottleResponse);

		// then
		mockMvc.perform(get("/api/v1/my-page/{userId}/my-bottle/picks", userId)
						.param("keyword", "")
						.param("regionId", "")
						.param("sortType", "LATEST")
						.param("sortOrder", "DESC")
						.param("cursor", "0")
						.param("pageSize", "50")
						.contentType(MediaType.APPLICATION_JSON)
						.with(csrf()))
				.andExpect(status().isOk())
				.andDo(document("user/mybottle",
						pathParameters(
								parameterWithName("userId").description("유저 아이디")
						),
						queryParameters(
								parameterWithName("keyword").optional().description("검색 키워드"),
								parameterWithName("regionId").optional().description("지역 ID"),
								parameterWithName("sortType").optional().description("정렬 타입(해당 문서 하단 enum 참조)"),
								parameterWithName("sortOrder").optional().description("정렬 순서(해당 문서 하단 enum 참조)"),
								parameterWithName("cursor").optional().description("조회 할 시작 기준 위치"),
								parameterWithName("pageSize").optional().description("조회 할 페이지 사이즈"),
								parameterWithName("_csrf").ignored()
						),
						responseFields(
								fieldWithPath("success").description("응답 성공 여부"),
								fieldWithPath("code").description("응답 코드"),
								fieldWithPath("data").description("응답 데이터"),
								fieldWithPath("data.userId").description("유저 아이디"),
								fieldWithPath("data.isMyPage").description("본인 여부"),
								fieldWithPath("data.totalCount").description("전체 보틀 수"),
								fieldWithPath("data.myBottleList").description("보틀 목록"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.alcoholId").description("알코올 아이디"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.alcoholKorName").description("알코올 한글명"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.alcoholEngName").description("알코올 영문명"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.korCategoryName").description("알코올 카테고리명"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.imageUrl").description("알코올 이미지 URL"),
								fieldWithPath("data.myBottleList[].baseMyBottleInfo.isHot5").description("HOT5 여부"),
								fieldWithPath("data.myBottleList[].isPicked").description("리뷰 ID").optional(),
								fieldWithPath("data.myBottleList[].totalPicksCount").description("찜 상태"),
								fieldWithPath("data.cursorPageable").description("전체 찜한 유저 수").optional(),
								fieldWithPath("errors").ignored(),
								fieldWithPath("meta.serverVersion").ignored(),
								fieldWithPath("meta.serverEncoding").ignored(),
								fieldWithPath("meta.serverResponseTime").ignored(),
								fieldWithPath("meta.serverPathVersion").ignored()
						)
				));
	}
}
