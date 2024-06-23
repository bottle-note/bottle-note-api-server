package app.bottlenote.docs.follow;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.follow.controller.FollowerController;
import app.bottlenote.follow.domain.constant.FollowStatus;
import app.bottlenote.follow.dto.response.FollowDetail;
import app.bottlenote.follow.dto.response.FollowSearchResponse;
import app.bottlenote.follow.fixture.FollowQueryFixture;
import app.bottlenote.follow.service.FollowerService;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("FollowerController RestDocs 테스트")
class RestDocsFollowerControllerTest extends AbstractRestDocs {

	private final FollowerService followerService = mock(FollowerService.class);
	private final FollowQueryFixture followQueryFixture = new FollowQueryFixture();
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	@Override
	protected Object initController() {
		return new FollowerController(followerService);
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
	@DisplayName("팔로워 리스트를 조회할 수 있다.")
	void docs_1() throws Exception {
		// given
		PageResponse<FollowSearchResponse> response = followQueryFixture.getPageResponse();

		// when
		when(followerService.findFollowerList(any(), any())).thenReturn(response);

		// then
		mockMvc.perform(get("/api/v1/follower/1")
				.param("cursor", "0")
				.param("pageSize", "50")
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(document("follower/search",
				queryParameters(
					parameterWithName("cursor").description("조회할 커서 위치"),
					parameterWithName("pageSize").description("페이지 크기"),
					parameterWithName("_csrf").ignored() // CSRF 토큰을 쿼리 매개변수로 추가
				),
				responseFields(
					fieldWithPath("success").description("응답 성공 여부"),
					fieldWithPath("code").description("응답 코드(http status code)"),
					fieldWithPath("data.totalCount").description("총 팔로워 수"),
					fieldWithPath("data.followList[].userId").description("팔로워의 아이디"),
					fieldWithPath("data.followList[].followUserId").description("팔로우 유저의 아이디"),
					fieldWithPath("data.followList[].nickName").description("팔로워의 닉네임"),
					fieldWithPath("data.followList[].userProfileImage").description("팔로워의 프로필 이미지 URL"),
					fieldWithPath("data.followList[].status").description("팔로우 상태"),
					fieldWithPath("data.followList[].reviewCount").description("리뷰 수"),
					fieldWithPath("data.followList[].ratingCount").description("평점 수"),
					fieldWithPath("errors").ignored(),
					fieldWithPath("meta.serverEncoding").ignored(),
					fieldWithPath("meta.serverVersion").ignored(),
					fieldWithPath("meta.serverPathVersion").ignored(),
					fieldWithPath("meta.serverResponseTime").ignored(),
					fieldWithPath("meta.pageable").description("페이징 정보"),
					fieldWithPath("meta.pageable.currentCursor").description("조회 시 기준 커서"),
					fieldWithPath("meta.pageable.cursor").description("다음 페이지 커서"),
					fieldWithPath("meta.pageable.pageSize").description("조회된 페이지 사이즈"),
					fieldWithPath("meta.pageable.hasNext").description("다음 페이지 존재 여부")
				)
			));
	}

}
