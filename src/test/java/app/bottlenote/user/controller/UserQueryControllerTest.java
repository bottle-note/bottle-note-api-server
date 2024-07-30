package app.bottlenote.user.controller;

import app.bottlenote.global.security.SecurityContextUtil;
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

		resultActions.andExpect(jsonPath("$.body.success").value("true"));
		resultActions.andExpect(jsonPath("$.body.code").value("200"));
		resultActions.andExpect(jsonPath("$.body.data.userId").value(1));
		resultActions.andExpect(jsonPath("$.body.data.nickName").value("nickname"));
		resultActions.andExpect(jsonPath("$.body.data.imageUrl").value("test.trl.com"));
		resultActions.andExpect(jsonPath("$.body.data.reviewCount").value(10));
		resultActions.andExpect(jsonPath("$.body.data.ratingCount").value(10));
		resultActions.andExpect(jsonPath("$.body.data.pickCount").value(10));
		resultActions.andExpect(jsonPath("$.body.data.followerCount").value(5));
		resultActions.andExpect(jsonPath("$.body.data.followingCount").value(3));
		resultActions.andExpect(jsonPath("$.body.data.isFollow").value(true));
		resultActions.andExpect(jsonPath("$.body.data.isMyPage").value(true));
	}
}
