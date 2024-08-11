package app.bottlenote.user.controller;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.fixture.UserQueryFixture;
import app.bottlenote.user.service.UserQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@DisplayName("[unit] [controller] UserQueryController")
@WebMvcTest(UserQueryController.class)
@WithMockUser
public class UserQueryControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper mapper;
	@MockBean
	private UserQueryService userQueryService;
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;
	private final UserQueryFixture mypageQueryFixture = new UserQueryFixture();

	@BeforeEach
	void setup() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
		mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(1L));
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@DisplayName("마이페이지 유저 정보를 조회할 수 있다.")
	@Test
	void test_1() throws Exception {
		// given
		Long userId = 1L;
		MyPageResponse myPageUserInfo = mypageQueryFixture.getMyPageInfo(1L, "nickname", "test.trl.com", 10L, 10L, 10L, 5L, 3L, true, true);

		// when
		when(userQueryService.getMypage(any(), any())).thenReturn(myPageUserInfo);

		// then
		ResultActions resultActions = mockMvc.perform(get("/api/v1/mypage/{userId}", userId))
			.andExpect(status().isOk())
			.andDo(print());

		resultActions.andExpect(jsonPath("$.success").value("true"));
		resultActions.andExpect(jsonPath("$.code").value("200"));
		resultActions.andExpect(jsonPath("$.data.userId").value(1));
		resultActions.andExpect(jsonPath("$.data.nickName").value("nickname"));
		resultActions.andExpect(jsonPath("$.data.imageUrl").value("test.trl.com"));
		resultActions.andExpect(jsonPath("$.data.reviewCount").value(10));
		resultActions.andExpect(jsonPath("$.data.ratingCount").value(10));
		resultActions.andExpect(jsonPath("$.data.pickCount").value(10));
		resultActions.andExpect(jsonPath("$.data.followerCount").value(5));
		resultActions.andExpect(jsonPath("$.data.followingCount").value(3));
		resultActions.andExpect(jsonPath("$.data.isFollow").value(true));
		resultActions.andExpect(jsonPath("$.data.isMyPage").value(true));
	}

	@DisplayName("마이 보틀 정보를 조회할 수 있다.")
	@Test
	void test_2() throws Exception {
		// given
		Long userId = 8L;
		List<MyBottleResponse.MyBottleInfo> myBottleList = List.of(
			new MyBottleResponse.MyBottleInfo(
				1L, "글렌피딕 12년", "Glenfiddich 12 Year Old", "싱글 몰트 위스키",
				"https://example.com/image1.jpg", true, 4.5, true
			),
			new MyBottleResponse.MyBottleInfo(
				2L, "맥캘란 18년", "Macallan 18 Year Old", "싱글 몰트 위스키",
				"https://example.com/image2.jpg", false, 0.0, false
			)
		);
		MyBottleResponse myBottleResponse = mypageQueryFixture.getMyBottleResponse(userId, true, myBottleList, null);

		// when
		when(userQueryService.getMyBottle(any(), any(), any())).thenReturn(myBottleResponse);

		// then
		ResultActions resultActions = mockMvc.perform(get("/api/v1/mypage/{userId}/my-bottle", userId)
				.param("keyword", "")
				.param("regionId", "")
				.param("tabType", "ALL")
				.param("sortType", "LATEST")
				.param("sortOrder", "DESC")
				.param("cursor", "0")
				.param("pageSize", "50"))
			.andExpect(status().isOk())
			.andDo(print());
		
		resultActions.andExpect(jsonPath("$.success").value("true"));
		resultActions.andExpect(jsonPath("$.code").value("200"));
		resultActions.andExpect(jsonPath("$.data.userId").value(8));
		resultActions.andExpect(jsonPath("$.data.isMyPage").value(true));
		resultActions.andExpect(jsonPath("$.data.totalCount").value(2));
		resultActions.andExpect(jsonPath("$.data.myBottleList[0].alcoholId").value(1));
		resultActions.andExpect(jsonPath("$.data.myBottleList[0].korName").value("글렌피딕 12년"));
		resultActions.andExpect(jsonPath("$.data.myBottleList[0].engName").value("Glenfiddich 12 Year Old"));
		resultActions.andExpect(jsonPath("$.data.myBottleList[0].korCategoryName").value("싱글 몰트 위스키"));
		resultActions.andExpect(jsonPath("$.data.myBottleList[0].imageUrl").value("https://example.com/image1.jpg"));
		resultActions.andExpect(jsonPath("$.data.myBottleList[0].isPicked").value(true));
		resultActions.andExpect(jsonPath("$.data.myBottleList[0].hasReviewByMe").value(true));
		resultActions.andExpect(jsonPath("$.data.myBottleList[0].rating").value(4.5));

		resultActions.andExpect(jsonPath("$.data.myBottleList[1].alcoholId").value(2));
		resultActions.andExpect(jsonPath("$.data.myBottleList[1].korName").value("맥캘란 18년"));
		resultActions.andExpect(jsonPath("$.data.myBottleList[1].engName").value("Macallan 18 Year Old"));
		resultActions.andExpect(jsonPath("$.data.myBottleList[1].korCategoryName").value("싱글 몰트 위스키"));
		resultActions.andExpect(jsonPath("$.data.myBottleList[1].imageUrl").value("https://example.com/image2.jpg"));
		resultActions.andExpect(jsonPath("$.data.myBottleList[1].isPicked").value(false));
		resultActions.andExpect(jsonPath("$.data.myBottleList[1].hasReviewByMe").value(false));
		resultActions.andExpect(jsonPath("$.data.myBottleList[1].rating").value(0.0));
	}
}
