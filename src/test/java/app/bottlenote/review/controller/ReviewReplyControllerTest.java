package app.bottlenote.review.controller;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.service.ReviewReplyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("리뷰 댓글 컨트롤러 레이어 테스트")
@WebMvcTest(ReviewReplyController.class)
@WithMockUser
class ReviewReplyControllerTest {

	@Autowired
	private ObjectMapper mapper;
	@Autowired
	private MockMvc mockMvc;
	@MockBean
	private ReviewReplyService reviewReplyService;
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;

	@BeforeEach
	void setup() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
	}

	@Nested
	@DisplayName("리뷰에 새로운 댓글을 등록할 수 있다.")
	class registerReviewReply {
		@Test
		@DisplayName("새로운 댓글을 등록 할 수 있다.")
		void test_1() throws Exception {
			final Long reviewId = 1L;
			var request = ReviewObjectFixture.getReviewReplyRegisterRequest();
			var response = ReviewObjectFixture.getReviewReplyResponse();

			mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(1L));
			when(reviewReplyService.registerReviewReply(1L, 1L, request)).thenReturn(response);

			mockMvc.perform(post("/api/v1/review/reply/register/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isOk());
		}
	}
}
