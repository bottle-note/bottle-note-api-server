package app.bottlenote.user.controller;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.fixture.UserQueryFixture;
import app.bottlenote.user.service.UserBasicService;
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
@WebMvcTest(UserMyPageController.class)
@WithMockUser
class UserQueryControllerTest {

	private final UserQueryFixture mypageQueryFixture = new UserQueryFixture();
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper mapper;
	@MockBean
	private UserBasicService userQueryService;
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

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
		MyPageResponse myPageUserInfo = mypageQueryFixture.getMyPageInfo();

		// when
		when(userQueryService.getMyPage(any(), any())).thenReturn(myPageUserInfo);

		// then
		ResultActions resultActions = mockMvc.perform(get("/api/v1/my-page/{userId}", userId))
			.andExpect(status().isOk())
			.andDo(print());

		resultActions.andExpect(jsonPath("$.success").value("true"));
		resultActions.andExpect(jsonPath("$.code").value("200"));
		resultActions.andExpect(jsonPath("$.data.userId").value(1));
		resultActions.andExpect(jsonPath("$.data.nickName").value("nickname"));
		resultActions.andExpect(jsonPath("$.data.imageUrl").value("imageUrl"));
		resultActions.andExpect(jsonPath("$.data.reviewCount").value(10));
		resultActions.andExpect(jsonPath("$.data.ratingCount").value(20));
		resultActions.andExpect(jsonPath("$.data.pickCount").value(30));
		resultActions.andExpect(jsonPath("$.data.followerCount").value(40));
		resultActions.andExpect(jsonPath("$.data.followingCount").value(50));
		resultActions.andExpect(jsonPath("$.data.isFollow").value(false));
		resultActions.andExpect(jsonPath("$.data.isMyPage").value(false));
	}

	@DisplayName("마이 보틀 정보를 조회할 수 있다.")
	@Test
	void test_2() throws Exception {
		// given
		Long userId = 8L;
		MyBottleResponse myBottleResponse = mypageQueryFixture.getMyBottleResponse(userId, true, null);

		// when
		when(userQueryService.getMyBottle(any(), any(), any())).thenReturn(myBottleResponse);

		// then
		ResultActions resultActions = mockMvc.perform(get("/api/v1/my-page/{userId}/my-bottle", userId)
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
	}
}
