package app.bottlenote.follow.controller;


import app.bottlenote.follow.domain.constant.FollowStatus;
import app.bottlenote.follow.dto.FollowUpdateRequest;
import app.bottlenote.follow.dto.FollowUpdateResponse;
import app.bottlenote.follow.exception.FollowException;
import app.bottlenote.follow.exception.FollowExceptionCode;
import app.bottlenote.follow.service.FollowCommandService;
import app.bottlenote.global.security.SecurityContextUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Optional;

import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FollowCommandController.class)
@WithMockUser
class FollowUpdateControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper mapper;
	@MockBean
	private FollowCommandService followCommandService;

	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	@BeforeEach
	void setup() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
		mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(9L));
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@DisplayName("다른 유저를 팔로우 할 수 있다.")
	@Test
	void shouldFollowOtherUser() throws Exception {

		// given
		FollowUpdateRequest request = new FollowUpdateRequest(1L, FollowStatus.FOLLOWING);
		FollowUpdateResponse response = FollowUpdateResponse.builder()
			.status(FollowStatus.FOLLOWING)
			.followUserId(1L)
			.nickName("nickName")
			.imageUrl("imageUrl")
			.build();

		// when
		when(followCommandService.updateFollowStatus(request, 9L)).thenReturn(response);


		// then
		ResultActions resultActions = mockMvc.perform(post("/api/v1/follow")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf())
			)
			.andExpect(status().isOk())
			.andDo(print());

		resultActions.andExpect(jsonPath("$.success").value("true"));
		resultActions.andExpect(jsonPath("$.code").value("200"));
		resultActions.andExpect(jsonPath("$.data.followUserId").value(response.getFollowUserId()));
		resultActions.andExpect(jsonPath("$.data.nickName").value(response.getNickName()));
		resultActions.andExpect(jsonPath("$.data.imageUrl").value(response.getImageUrl()));
		resultActions.andExpect(jsonPath("$.data.message").value(response.getMessage()));

	}

	@DisplayName("유저를 언팔로우할 수 있다.")
	@Test
	void shouldUnfollowUser() throws Exception {
		// given
		FollowUpdateRequest request = new FollowUpdateRequest(1L, FollowStatus.UNFOLLOW);
		FollowUpdateResponse response = FollowUpdateResponse.builder()
			.status(FollowStatus.UNFOLLOW)
			.followUserId(1L)
			.nickName("nickName")
			.imageUrl("imageUrl")
			.build();

		// when
		when(followCommandService.updateFollowStatus(request, 9L)).thenReturn(response);

		// then
		ResultActions resultActions = mockMvc.perform(post("/api/v1/follow")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(print());

		resultActions.andExpect(jsonPath("$.success").value("true"));
		resultActions.andExpect(jsonPath("$.code").value("200"));
		resultActions.andExpect(jsonPath("$.data.followUserId").value(response.getFollowUserId()));
		resultActions.andExpect(jsonPath("$.data.nickName").value(response.getNickName()));
		resultActions.andExpect(jsonPath("$.data.imageUrl").value(response.getImageUrl()));
		resultActions.andExpect(jsonPath("$.data.message").value(response.getMessage()));

	}

	@DisplayName("자기 자신을 팔로우할 수 없다.")
	@Test
	void shouldNotFollowSelf() throws Exception {
		// given
		FollowUpdateRequest request = new FollowUpdateRequest(9L, FollowStatus.FOLLOWING);

		// when
		when(followCommandService.updateFollowStatus(request, 9L))
			.thenThrow(new FollowException(FollowExceptionCode.CANNOT_FOLLOW_SELF));

		// then
		ResultActions resultActions = mockMvc.perform(post("/api/v1/follow")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andDo(print());

		resultActions.andExpect(jsonPath("$.success").value(false));
		resultActions.andExpect(jsonPath("$.code").value(400));
		resultActions.andExpect(jsonPath("$.errors.message").value("자기 자신을 팔로우, 언팔로우 할 수 없습니다."));
	}


	@DisplayName("팔로우할 유저가 존재하지 않으면 팔로우할 수 없다.")
	@Test
	void shouldNotFollowNotFoundUser() throws Exception {
		// given
		FollowUpdateRequest request = new FollowUpdateRequest(1L, FollowStatus.FOLLOWING);

		// when
		when(followCommandService.updateFollowStatus(request, 9L))
			.thenThrow(new FollowException(FollowExceptionCode.FOLLOW_NOT_FOUND));

		// then
		ResultActions resultActions = mockMvc.perform(post("/api/v1/follow")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isBadRequest())
			.andDo(print());

		resultActions.andExpect(jsonPath("$.success").value(false));
		resultActions.andExpect(jsonPath("$.code").value(400));
		resultActions.andExpect(jsonPath("$.errors.message").value("팔로우할 대상을 찾을 수 없습니다."));
	}

}
