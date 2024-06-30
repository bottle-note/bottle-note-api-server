package app.bottlenote.docs.review;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.review.controller.ReviewReplyController;
import app.bottlenote.review.fixture.ReviewObjectFixture;
import app.bottlenote.review.service.ReviewReplyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("리뷰 댓글 컨트롤러 RestDocs용 테스트")
class RestReviewReplyControllerTest extends AbstractRestDocs {

	private final ReviewReplyService reviewReplyService = mock(ReviewReplyService.class);
	private MockedStatic<SecurityContextUtil> mockedSecurityUtil;


	@Override
	protected Object initController() {
		return new ReviewReplyController(reviewReplyService);
	}

	@BeforeEach
	void setup() {
		mockedSecurityUtil = mockStatic(SecurityContextUtil.class);
	}

	@AfterEach
	void tearDown() {
		mockedSecurityUtil.close();
	}

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
				.content(objectMapper.writeValueAsString(request))
				.with(csrf())
			)
			.andDo(print())
			.andExpect(status().isOk())
			.andDo(
				document("review/reply/register",
					pathParameters(
						parameterWithName("reviewId").description("댓글을 작성 대상 리뷰의 식별자")
					),
					requestFields(
						fieldWithPath("content").description("등록할 댓글 내용 본문"),
						fieldWithPath("parentReplyId").optional().description("상위 댓글 식별자")
					),
					responseFields(
						fieldWithPath("success").description("요청 성공 여부"),
						fieldWithPath("code").description("응답 코드"),
						fieldWithPath("data").description("응답 데이터"),
						fieldWithPath("data.codeMessage").description("결과 코드 메시지"),
						fieldWithPath("data.message").description("결과 메시지"),
						fieldWithPath("data.reviewId").description("대상 리뷰 식별자"),
						fieldWithPath("data.responseAt").description("응답 일시"),
						fieldWithPath("errors").description("에러 목록"),
						fieldWithPath("meta").description("메타 정보"),
						fieldWithPath("meta.serverVersion").ignored(),
						fieldWithPath("meta.serverEncoding").ignored(),
						fieldWithPath("meta.serverResponseTime").ignored(),
						fieldWithPath("meta.serverPathVersion").ignored()
					)
				)
			);
	}

}
