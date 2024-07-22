package app.bottlenote.docs.likes;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.like.controller.LikesCommandController;
import app.bottlenote.like.domain.LikeStatus;
import app.bottlenote.like.dto.request.LikesUpdateRequest;
import app.bottlenote.like.dto.response.LikesUpdateResponse;
import app.bottlenote.like.service.LikesCommandService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@DisplayName("RestLikesController API 문서화 테스트")
public class RestLikesControllerDocsTest extends AbstractRestDocs {

	private final Long userId = 99L;
	private final MockedStatic<SecurityContextUtil> mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
	private final LikesCommandService likesCommandService = mock(LikesCommandService.class);

	@Override
	protected Object initController() {
		return new LikesCommandController(likesCommandService);
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}


	@Test
	@DisplayName("좋아요를 등록할 수 있다.")
	void updateLikes() throws Exception {
		// given
		final Long reviewId = 1L;
		var request = new LikesUpdateRequest(1L, LikeStatus.LIKE);
		var response = LikesUpdateResponse.of(1L, reviewId, userId, "하오하오상", LikeStatus.LIKE);

		// when
		when(SecurityContextUtil.getUserIdByContext()).thenReturn(Optional.of(userId));
		when(likesCommandService.updateLikes(userId, reviewId, LikeStatus.LIKE)).thenReturn(response);

		// then
		mockMvc.perform(put("/api/v1/likes")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))
				.with(csrf()))
			.andExpect(status().isOk())
			.andDo(
				document("review/likes/update",
					requestFields(
						fieldWithPath("reviewId").type(NUMBER).description("좋아요 대상 리뷰의 식별자"),
						fieldWithPath("status").type(STRING).description("업데이트 할 상태 값 하단 참조")
					),
					responseFields(
						fieldWithPath("success").ignored(),
						fieldWithPath("code").ignored(),
						fieldWithPath("data").ignored(),
						fieldWithPath("data.message").description("결과 메시지"),
						fieldWithPath("data.likesId").description("좋아요 결과 값 식별자"),
						fieldWithPath("data.reviewId").description("좋아요 대상 리뷰의 식별자"),
						fieldWithPath("data.userId").description("좋아요를 등록한 사용자 식별자"),
						fieldWithPath("data.userNickName").description("좋아요를 등록한 사용자 닉네임"),
						fieldWithPath("data.status").description("업데이트 된 좋아요 상태 값"),
						fieldWithPath("errors").ignored(),
						fieldWithPath("meta").ignored(),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored()
					)
				)
			)
			.andDo(print());
	}
}
