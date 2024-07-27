package app.bottlenote.review.controller;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.review.fixture.ReviewReplyObjectFixture;
import app.bottlenote.review.service.ReviewReplyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@DisplayName("[unit] [controller] ReviewReplyController")
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

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

	@Nested
	@DisplayName("리뷰에 새로운 댓글을 등록할 수 있다.")
	class registerReviewReply {
		@Test
		@DisplayName("새로운 댓글을 등록 할 수 있다.")
		void test_1() throws Exception {
			final Long reviewId = 1L;
			var request = ReviewReplyObjectFixture.getReviewReplyRegisterRequest();
			var response = ReviewReplyObjectFixture.getReviewReplyResponse();

			mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(1L));
			when(reviewReplyService.registerReviewReply(1L, 1L, request)).thenReturn(response);

			mockMvc.perform(post("/api/v1/review/reply/register/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.codeMessage").value("SUCCESS_REGISTER_REPLY"))
				.andExpect(jsonPath("$.data.message").value("성공적으로 댓글을 등록했습니다."))
				.andExpect(jsonPath("$.data.reviewId").value("1"));
		}

		@Test
		@DisplayName("댓글 내용이 없는 경우 예외가 반환된다.")
		void test_2() throws Exception {
			final Long reviewId = 1L;
			var request = ReviewReplyObjectFixture.getReviewReplyRegisterRequest(null, null);

			mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(1L));

			mockMvc.perform(post("/api/v1/review/reply/register/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.content", containsString("댓글 내용은 필수 입력값입니다.")));

		}

		@Test
		@DisplayName("댓글 내용이 500자를 초과하는 경우 예외가 반환된다.")
		void test_3() throws Exception {
			final Long reviewId = 1L;
			var request = ReviewReplyObjectFixture.getReviewReplyRegisterRequest(RandomStringUtils.randomAlphabetic(501), null);

			mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(1L));

			mockMvc.perform(post("/api/v1/review/reply/register/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.content(mapper.writeValueAsString(request))
					.with(csrf())
				)
				.andDo(print())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.content", containsString("댓글 내용은 1자 이상 500자 이하로 작성해주세요.")));
		}
	}
}
