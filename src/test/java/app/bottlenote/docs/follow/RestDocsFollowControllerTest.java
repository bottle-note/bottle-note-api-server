package app.bottlenote.docs.follow;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.follow.controller.FollowController;
import app.bottlenote.follow.domain.constant.FollowStatus;
import app.bottlenote.follow.dto.request.FollowUpdateRequest;
import app.bottlenote.follow.dto.response.FollowDetail;
import app.bottlenote.follow.dto.response.FollowSearchResponse;
import app.bottlenote.follow.dto.response.FollowUpdateResponse;
import app.bottlenote.follow.fixture.FollowQueryFixture;
import app.bottlenote.follow.service.FollowService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("FollowController RestDocs 테스트")
class RestDocsFollowControllerTest extends AbstractRestDocs {

	private final FollowService followService = mock(FollowService.class);
	private final FollowQueryFixture followQueryFixture = new FollowQueryFixture();
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	@Override
	protected Object initController() {
		return new FollowController(followService);
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
	@DisplayName("팔로우 할 수 있다.")
	void docs_1() throws Exception {
		// given
		FollowUpdateRequest request = new FollowUpdateRequest(1L, FollowStatus.FOLLOWING);
		FollowUpdateResponse response = FollowUpdateResponse.builder()
			.status(FollowStatus.FOLLOWING)
			.followUserId(1L)
			.nickName("nickName")
			.imageUrl("imageUrl")
			.build();

		// when
		when(followService.updateFollowStatus(request, 9L)).thenReturn(response);

		// then
		mockMvc.perform(post("/api/v1/follow")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(document("follow/update",
				requestFields(
					fieldWithPath("followUserId").type(JsonFieldType.NUMBER).description("팔로우할 유저의 아이디"),
					fieldWithPath("status").type(JsonFieldType.STRING).description("팔로우 상태")
				),
				responseFields(
					fieldWithPath("success").type(JsonFieldType.BOOLEAN).description("응답 성공 여부"),
					fieldWithPath("code").type(JsonFieldType.NUMBER).description("응답 코드(http status code)"),
					fieldWithPath("data.followUserId").type(JsonFieldType.NUMBER).description("팔로우 유저의 아이디"),
					fieldWithPath("data.nickName").type(JsonFieldType.STRING).description("팔로우 유저의 닉네임"),
					fieldWithPath("data.imageUrl").type(JsonFieldType.STRING).description("팔로우 유저프로필 이미지 URL"),
					fieldWithPath("data.message").type(JsonFieldType.STRING).description("메시지"),
					fieldWithPath("errors").type(JsonFieldType.ARRAY).description("응답 성공 여부가 false일 경우 에러 메시지(없을 경우 null)"),
					fieldWithPath("meta.serverEncoding").ignored(),
					fieldWithPath("meta.serverVersion").ignored(),
					fieldWithPath("meta.serverPathVersion").ignored(),
					fieldWithPath("meta.serverResponseTime").ignored()
				)
			));
	}

	@Test
	@DisplayName("팔로우 리스트를 조회할 수 있다.")
	void docs_2() throws Exception {
		// given
		PageResponse<FollowSearchResponse> response = followQueryFixture.getPageResponse();

		// when
		when(followService.findFollowList(any(), any())).thenReturn(response);

		// then
		mockMvc.perform(get("/api/v1/follow/1")
				.param("cursor", "0")
				.param("pageSize", "50")
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(document("follow/search",
				queryParameters(
					parameterWithName("cursor").optional().description("조회 할 시작 기준 위치"),
					parameterWithName("pageSize").optional().description("조회 할 페이지 사이즈"),
					parameterWithName("_csrf").ignored() // CSRF 토큰을 쿼리 매개변수로 추가
				),
				responseFields(
					fieldWithPath("success").description("응답 성공 여부"),
					fieldWithPath("code").description("응답 코드(http status code)"),
					fieldWithPath("data.totalCount").description("총 팔로우 수"),
					fieldWithPath("data.followList[].userId").description("팔로우한 유저의 아이디"),
					fieldWithPath("data.followList[].followUserId").description("팔로우 유저의 아이디"),
					fieldWithPath("data.followList[].nickName").description("팔로우한 유저의 닉네임"),
					fieldWithPath("data.followList[].userProfileImage").description("팔로우한 유저의 프로필 이미지 URL"),
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
