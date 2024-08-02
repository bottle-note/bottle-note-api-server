package app.bottlenote.docs.review;

import app.bottlenote.docs.AbstractRestDocs;
import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.review.controller.ReviewReplyController;
import app.bottlenote.review.service.ReviewReplyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;

import static app.bottlenote.review.fixture.ReviewReplyObjectFixture.getDeleteReviewReplyResponse;
import static app.bottlenote.review.fixture.ReviewReplyObjectFixture.getReviewReplyInfoList;
import static app.bottlenote.review.fixture.ReviewReplyObjectFixture.getReviewReplyRegisterRequest;
import static app.bottlenote.review.fixture.ReviewReplyObjectFixture.getReviewReplyResponse;
import static app.bottlenote.review.fixture.ReviewReplyObjectFixture.getSubReviewReplyInfo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("rest-docs")
@DisplayName("리뷰 댓글 컨트롤러 RestDocs용 테스트")
class RestReviewReplyControllerTest extends AbstractRestDocs {

	private static final Logger log = LogManager.getLogger(RestReviewReplyControllerTest.class);
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

	@Nested
	@DisplayName("댓글 조회")
	class select {

		@Test
		@DisplayName("새로운 댓글을 등록 할 수 있다.")
		void test_1() throws Exception {
			final Long reviewId = 1L;
			var request = getReviewReplyRegisterRequest("이 리뷰는 매우 유익합니다. 추천을 줄만하네요.");
			var response = getReviewReplyResponse();

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

		@Test
		@DisplayName("최상위 댓글 목록을 조회 할 수 있다.")
		void test_2() throws Exception {
			var reviewReplyInfoList = getReviewReplyInfoList(5);

			when(reviewReplyService.getReviewRootReplays(1L, 0L, 50L)).thenReturn(reviewReplyInfoList);

			Long reviewId = 1L;
			mockMvc.perform(get("/api/v1/review/reply/{reviewId}", reviewId)
					.contentType(MediaType.APPLICATION_JSON)
					.param("cursor", "0")
					.param("pageSize", "50"))
				.andExpect(status().isOk())
				.andDo(
					document("review/reply/list",
						pathParameters(
							parameterWithName("reviewId").description("댓글을 작성 대상 리뷰의 식별자")
						),
						queryParameters(
							parameterWithName("cursor").description("페이징 커서 (조회를 시작할 위치) "),
							parameterWithName("pageSize").description("페이지 크기 (한번에 조회될 데이터 양)")
						),
						responseFields(
							fieldWithPath("success").description("요청 성공 여부"),
							fieldWithPath("code").description("응답 코드"),
							fieldWithPath("data").description("응답 데이터"),
							fieldWithPath("data[].userId").description("댓글 작성자 식별자"),
							fieldWithPath("data[].imageUrl").description("댓글 작성자 프로필 이미지 URL"),
							fieldWithPath("data[].nickName").description("댓글 작성자 닉네임"),
							fieldWithPath("data[].reviewReplyId").description("댓글 자체 식별자"),
							fieldWithPath("data[].reviewReplyContent").description("댓글 내용"),
							fieldWithPath("data[].subReplyCount").description("하위 댓글 갯수"),
							fieldWithPath("data[].createAt").description("댓글 작성 일시"),
							fieldWithPath("errors").ignored(),
							fieldWithPath("meta").ignored(),
							fieldWithPath("meta.serverVersion").ignored(),
							fieldWithPath("meta.serverEncoding").ignored(),
							fieldWithPath("meta.serverResponseTime").ignored(),
							fieldWithPath("meta.serverPathVersion").ignored()
						)
					)
				);
		}

		@Test
		@DisplayName("대댓글 목록을 조회 할 수 있다.")
		void test_3() throws Exception {
			long index = 0L;
			final Long userId = 123L;
			final String authorName = "썩어가는망고";
			final String extraAuthorName = "신선한반나나";
			final Long reviewId = 1L;
			final Long rootReplyId = 1L;

			var reviewReplyInfoList = List.of(
				getSubReviewReplyInfo(userId, ++index, 1L, 1L, authorName),
				getSubReviewReplyInfo(userId, ++index, 1L, index - 1, extraAuthorName),
				getSubReviewReplyInfo(userId, ++index, 1L, index - 2, extraAuthorName),
				getSubReviewReplyInfo(userId, ++index, 1L, 1L, authorName)
			);

			when(reviewReplyService.getSubReviewReplies(1L, 1L, 0L, 50L)).thenReturn(reviewReplyInfoList);

			mockMvc.perform(get("/api/v1/review/reply/{reviewId}/sub/{rootReplyId}", reviewId, rootReplyId)
					.contentType(MediaType.APPLICATION_JSON)
					.param("cursor", "0")
					.param("pageSize", "50"))
				.andExpect(status().isOk())
				.andDo(
					document("review/reply/sub-list",
						pathParameters(
							parameterWithName("reviewId").description("댓글을 작성 대상 리뷰의 식별자"),
							parameterWithName("rootReplyId").description("최상위 댓글 식별자")
						),
						queryParameters(
							parameterWithName("cursor").description("페이징 커서 (조회를 시작할 위치) "),
							parameterWithName("pageSize").description("페이지 크기 (한번에 조회될 데이터 양)")
						),
						responseFields(
							fieldWithPath("success").description("요청 성공 여부"),
							fieldWithPath("code").description("응답 코드"),
							fieldWithPath("data").description("응답 데이터"),
							fieldWithPath("data[].userId").description("댓글 작성자 식별자"),
							fieldWithPath("data[].imageUrl").description("댓글 작성자 프로필 이미지 URL"),
							fieldWithPath("data[].nickName").description("댓글 작성자 닉네임"),
							fieldWithPath("data[].rootReviewId").description("최상위 댓글 식별자"),
							fieldWithPath("data[].parentReviewReplyId").description("상위 댓글 식별자"),
							fieldWithPath("data[].parentReviewReplyAuthor").description("상위 댓글 작성자 닉네임 (@ 기호는 제외)"),
							fieldWithPath("data[].reviewReplyId").description("댓글 자체 식별자"),
							fieldWithPath("data[].reviewReplyContent").description("댓글 내용"),
							fieldWithPath("data[].createAt").description("댓글 작성 일시"),
							fieldWithPath("errors").ignored(),
							fieldWithPath("meta").description("메타 정보 (해당 응답 값은 별도의 페이지 정보가 제공되지 않습니다.)"),
							fieldWithPath("meta.serverVersion").ignored(),
							fieldWithPath("meta.serverEncoding").ignored(),
							fieldWithPath("meta.serverResponseTime").ignored(),
							fieldWithPath("meta.serverPathVersion").ignored()
						)
					)
				);
		}

	}

	@Nested
	@DisplayName("댓글 삭제")
	class delete {
		@Test
		@DisplayName("댓글을 삭제 할 수 있다.")
		void test_1() throws Exception {
			final Long reviewId = 1L;
			final Long replyId = 1L;

			var response = getDeleteReviewReplyResponse(reviewId);

			mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(1L));
			when(reviewReplyService.deleteReviewReply(1L, 1L, 1L)).thenReturn(response);

			mockMvc.perform(delete("/api/v1/review/reply/{reviewId}/{replyId}", reviewId, replyId).with(csrf()))
				.andDo(print())
				.andExpect(status().isOk())
				.andDo(
					document("review/reply/delete",
						pathParameters(
							parameterWithName("reviewId").description("댓글을 작성 대상 리뷰의 식별자"),
							parameterWithName("replyId").description("삭제할 리뷰 식별자.")
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
}
