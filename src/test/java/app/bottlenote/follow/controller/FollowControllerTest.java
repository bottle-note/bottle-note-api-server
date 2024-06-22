package app.bottlenote.follow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.follow.dto.request.FollowPageableRequest;
import app.bottlenote.follow.dto.response.FollowSearchResponse;
import app.bottlenote.follow.fixture.FollowQueryFixture;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.follow.service.FollowService;
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

import java.util.stream.Stream;

@WithMockUser()
@DisplayName("팔로우 조회 컨트롤러 테스트")
@WebMvcTest(FollowController.class)
class FollowControllerTest {

	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected MockMvc mockMvc;

	@MockBean
	private FollowService followService;

	private final FollowQueryFixture followQueryFixture = new FollowQueryFixture();


	static Stream<Arguments> testCaseProvider() {
		return Stream.of(
			Arguments.of(1L, 0L, 50L),
			Arguments.of(2L, 10L, 50L),
			Arguments.of(3L, 20L, 50L)
		);
	}

	@DisplayName("팔로우 리스트를 조회할 수 있다.")
	@ParameterizedTest(name = "[{index}] userId: {0}, cursor: {1}, pageSize: {2}")
	@MethodSource("testCaseProvider")
	void testFindFollowList(Long userId, Long cursor, Long pageSize) throws Exception {
		// given
		PageResponse<FollowSearchResponse> response = followQueryFixture.getPageResponse();
		FollowPageableRequest pageableRequest = FollowPageableRequest.builder()
			.cursor(cursor)
			.pageSize(pageSize)
			.build();

		// when
		when(followService.findFollowList(any(), any())).thenReturn(response);

		// then
		ResultActions resultActions = mockMvc.perform(get("/api/v1/follow/{userId}", userId)
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
