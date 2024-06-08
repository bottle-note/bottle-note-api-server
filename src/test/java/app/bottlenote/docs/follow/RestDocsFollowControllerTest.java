package app.bottlenote.docs.follow;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.follow.controller.FollowCommandController;
import app.bottlenote.follow.domain.constant.FollowStatus;
import app.bottlenote.follow.dto.request.FollowUpdateRequest;
import app.bottlenote.follow.dto.response.FollowUpdateResponse;
import app.bottlenote.follow.service.FollowCommandService;
import app.bottlenote.global.security.SecurityContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("유저 팔로우 RestDocs용 테스트")
class RestDocsFollowControllerTest extends AbstractRestDocs {

	private final FollowCommandService followCommandService = mock(FollowCommandService.class);
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	@Override
	protected Object initController() {
		return new FollowCommandController(followCommandService);
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
	@DisplayName("닉네임 변경을 할 수 있다.")
	void changeNickname_test() throws Exception {
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

}

