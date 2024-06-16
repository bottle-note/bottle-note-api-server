package app.bottlenote.follow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.follow.domain.constant.FollowStatus;
import app.bottlenote.follow.dto.request.FollowPageableRequest;
import app.bottlenote.follow.dto.response.FollowDetail;
import app.bottlenote.follow.dto.response.FollowSearchResponse;
import app.bottlenote.follow.service.FollowCommandService;
import app.bottlenote.global.service.cursor.CursorPageable;
import app.bottlenote.global.service.cursor.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.stream.Stream;

@WithMockUser()
@DisplayName("팔로우 및 팔로워 조회 컨트롤러 테스트")
@WebMvcTest(FollowCommandController.class)
class FollowCommandControllerTest {

	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;
	@MockBean
	private FollowCommandService followCommandService;

	private PageResponse<FollowSearchResponse> getPageResponse() {
		List<FollowDetail> followDetails = List.of(
			FollowDetail.builder()
				.userId(1L)
				.followUserId(1L)
				.nickName("nickName1")
				.userProfileImage("imageUrl1")
				.status(FollowStatus.FOLLOWING)
				.reviewCount(10L)
				.ratingCount(5L)
				.build(),
			FollowDetail.builder()
				.userId(2L)
				.followUserId(1L)
				.nickName("nickName2")
				.userProfileImage("imageUrl2")
				.status(FollowStatus.FOLLOWING)
				.reviewCount(20L)
				.ratingCount(10L)
				.build(),
			FollowDetail.builder()
				.userId(3L)
				.followUserId(1L)
				.nickName("nickName3")
				.userProfileImage("imageUrl3")
				.status(FollowStatus.FOLLOWING)
				.reviewCount(30L)
				.ratingCount(15L)
				.build()
		);

		FollowSearchResponse followSearchResponse = FollowSearchResponse.of(5L, followDetails);

		CursorPageable cursorPageable = CursorPageable.builder()
			.cursor(0L)
			.pageSize(50L)
			.hasNext(false)
			.build();

		return PageResponse.of(followSearchResponse, cursorPageable);
	}

	static Stream<Arguments> testCaseProvider() {
		return Stream.of(
			Arguments.of(1L, 0L, 50L),
			Arguments.of(2L, 10L, 50L),
			Arguments.of(3L, 20L, 50L)
		);
	}

	@DisplayName("팔로워 리스트를 조회할 수 있다.")
	@ParameterizedTest(name = "[{index}] userId: {0}, cursor: {1}, pageSize: {2}")
	@MethodSource("testCaseProvider")
	void test_1 (Long userId, Long cursor, Long pageSize) throws Exception {
		// given
		PageResponse<FollowSearchResponse> response = getPageResponse();
		FollowPageableRequest pageableRequest = FollowPageableRequest.builder()
			.cursor(cursor)
			.pageSize(pageSize)
			.build();

		// when
		when(followCommandService.findFollowerList(any(), any())).thenReturn(response);

		// then
		ResultActions resultActions = mockMvc.perform(get("/api/v1/follow/{userId}/follower", userId)
				.param("cursor", pageableRequest.cursor().toString())
				.param("pageSize", pageableRequest.pageSize().toString())
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(print());

		resultActions.andExpect(jsonPath("$.success").value("true"));
		resultActions.andExpect(jsonPath("$.code").value("200"));
		resultActions.andExpect(jsonPath("$.data.totalCount").value(5));
		resultActions.andExpect(jsonPath("$.data.followList.size()").value(3));
	}

	@DisplayName("팔로우 리스트를 조회할 수 있다.")
	@ParameterizedTest(name = "[{index}] userId: {0}, cursor: {1}, pageSize: {2}")
	@MethodSource("testCaseProvider")
	void test_2(Long userId, Long cursor, Long pageSize) throws Exception {
		// given
		PageResponse<FollowSearchResponse> response = getPageResponse();
		FollowPageableRequest pageableRequest = FollowPageableRequest.builder()
			.cursor(cursor)
			.pageSize(pageSize)
			.build();

		// when
		when(followCommandService.findFollowList(any(), any())).thenReturn(response);

		// then
		ResultActions resultActions = mockMvc.perform(get("/api/v1/follow/{userId}/follow", userId)
				.param("cursor", pageableRequest.cursor().toString())
				.param("pageSize", pageableRequest.pageSize().toString())
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(print());

		resultActions.andExpect(jsonPath("$.success").value("true"));
		resultActions.andExpect(jsonPath("$.code").value("200"));
		resultActions.andExpect(jsonPath("$.data.totalCount").value(5));
		resultActions.andExpect(jsonPath("$.data.followList.size()").value(3));
	}
}
