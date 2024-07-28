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

@DisplayName("UserQueryController RestDocs 테스트")
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
		if (mockedSecurityUtil != null) {
			mockedSecurityUtil.close();
		}
	}

	@Test
	@DisplayName("마이페이지 유저 정보를 조회할 수 있다.")
	void test_1() throws Exception {
		// given
		Long userId = 1L;
		MyPageResponse myPageUserInfo = mypageQueryFixture.getMyPageInfo();

		// when
		when(userQueryService.getMypage(any(), any())).thenReturn(myPageUserInfo);

		// then
		mockMvc.perform(get("/api/v1/mypage/{userId}", userId)
				.contentType(MediaType.APPLICATION_JSON)
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(document("user/mypage",
				responseFields(
					fieldWithPath("headers").description("HTTP 헤더 정보"),
					fieldWithPath("body.success").description("응답 성공 여부"),
					fieldWithPath("body.code").description("응답 코드(http status code)"),
					fieldWithPath("body.data.userId").description("유저 아이디"),
					fieldWithPath("body.data.nickName").description("유저 닉네임"),
					fieldWithPath("body.data.imageUrl").description("유저 프로필 이미지 URL"),
					fieldWithPath("body.data.reviewCount").description("리뷰 수"),
					fieldWithPath("body.data.ratingCount").description("평점 수"),
					fieldWithPath("body.data.pickCount").description("찜한 수"),
					fieldWithPath("body.data.followerCount").description("팔로워 수"),
					fieldWithPath("body.data.followingCount").description("팔로잉 수"),
					fieldWithPath("body.data.isFollow").description("팔로우 여부"),
					fieldWithPath("body.data.isMyPage").description("본인 여부"),
					fieldWithPath("body.errors").description("에러 정보"),
					fieldWithPath("body.meta.serverVersion").description("서버 버전"),
					fieldWithPath("body.meta.serverEncoding").description("서버 인코딩"),
					fieldWithPath("body.meta.serverResponseTime").description("서버 응답 시간"),
					fieldWithPath("body.meta.serverPathVersion").description("서버 경로 버전"),
					fieldWithPath("statusCodeValue").description("HTTP 상태 코드 값"),
					fieldWithPath("statusCode").description("HTTP 상태 코드")
				)
			));
	}
}
