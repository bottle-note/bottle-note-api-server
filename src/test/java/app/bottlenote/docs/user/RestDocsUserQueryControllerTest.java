package app.bottlenote.docs.user;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.user.controller.UserQueryController;
import app.bottlenote.user.dto.response.MyPageResponse;
import app.bottlenote.user.fixture.UserQueryFixture;
import app.bottlenote.user.service.UserQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("[restdocs] 마이페이지 컨트롤러 RestDocs용 테스트")
public class RestDocsUserQueryControllerTest extends AbstractRestDocs {

	private final UserQueryService userQueryService = mock(UserQueryService.class);
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;
	private final UserQueryFixture mypageQueryFixture = new UserQueryFixture();

	@Override
	protected Object initController() {
		return new UserQueryController(userQueryService);
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
		MyPageResponse myPageUserInfo = mypageQueryFixture.getMyPageInfo(1L, "nickname", "test.trl.com", 10L, 10L, 10L, 5L, 3L, true, true);

		// when
		when(userQueryService.getMypage(any(), any())).thenReturn(myPageUserInfo);

		// then
		mockMvc.perform(get("/api/v1/mypage/{userId}", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(document("user/mypage",
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
}
