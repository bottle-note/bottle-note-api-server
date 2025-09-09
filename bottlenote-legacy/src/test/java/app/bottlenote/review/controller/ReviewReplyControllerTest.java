package app.bottlenote.review.controller;

import static app.bottlenote.review.fixture.ReviewReplyObjectFixture.getDeleteReviewReplyResponse;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.global.security.SecurityContextUtil;
import app.bottlenote.review.constant.ReviewReplyResultMessage;
import app.bottlenote.review.exception.ReviewException;
import app.bottlenote.review.exception.ReviewExceptionCode;
import app.bottlenote.review.fixture.ReviewReplyObjectFixture;
import app.bottlenote.review.service.ReviewReplyService;
import app.bottlenote.shared.data.response.Error;
import app.bottlenote.shared.exception.custom.code.ValidExceptionCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@Tag("unit")
@DisplayName("[unit] [controller] ReviewReplyController")
@WebMvcTest(ReviewReplyController.class)
@ActiveProfiles("test")
@WithMockUser
class ReviewReplyControllerTest {

  private static final Logger log = LogManager.getLogger(ReviewReplyControllerTest.class);
  @Autowired private ObjectMapper mapper;
  @Autowired private MockMvc mockMvc;
  @MockBean private ReviewReplyService reviewReplyService;
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

      mockMvc
          .perform(
              post("/api/v1/review/reply/register/{reviewId}", reviewId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(mapper.writeValueAsString(request))
                  .with(csrf()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.codeMessage").value("SUCCESS_REGISTER_REPLY"))
          .andExpect(jsonPath("$.data.message").value("성공적으로 댓글을 등록했습니다."))
          .andExpect(jsonPath("$.data.reviewId").value("1"));
    }

    @Test
    @DisplayName("댓글 내용이 없는 경우 예외가 반환된다.")
    void test_2() throws Exception {

      Error error = Error.of(ValidExceptionCode.REQUIRED_REVIEW_REPLY_CONTENT);

      final Long reviewId = 1L;
      var request = ReviewReplyObjectFixture.getReviewReplyRegisterRequest(null, null);

      mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(1L));

      mockMvc
          .perform(
              post("/api/v1/review/reply/register/{reviewId}", reviewId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(mapper.writeValueAsString(request))
                  .with(csrf()))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andDo(print())
          .andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
          .andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
          .andExpect(jsonPath("$.errors[0].message").value(error.message()));
    }

    @Test
    @DisplayName("댓글 내용이 500자를 초과하는 경우 예외가 반환된다.")
    void test_3() throws Exception {

      Error error = Error.of(ValidExceptionCode.CONTENT_IS_OUT_OF_RANGE);
      final Long reviewId = 1L;
      var request =
          ReviewReplyObjectFixture.getReviewReplyRegisterRequest(
              RandomStringUtils.randomAlphabetic(501), null);

      mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(1L));

      mockMvc
          .perform(
              post("/api/v1/review/reply/register/{reviewId}", reviewId)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(mapper.writeValueAsString(request))
                  .with(csrf()))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andDo(print())
          .andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
          .andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
          .andExpect(jsonPath("$.errors[0].message").value(error.message()));
    }
  }

  @Nested
  @DisplayName("리뷰 댓글을 삭제할 수 있다.")
  class delete {

    @Test
    @DisplayName("댓글을 삭제 할 수 있다.")
    void test_1() throws Exception {
      final Long reviewId = 1L;
      final Long replyId = 1L;

      var response = getDeleteReviewReplyResponse(reviewId);

      mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(1L));

      when(reviewReplyService.deleteReviewReply(1L, 1L, 1L)).thenReturn(response);

      mockMvc
          .perform(
              delete("/api/v1/review/reply/{reviewId}/{replyId}", reviewId, replyId).with(csrf()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(
              jsonPath("$.data.codeMessage")
                  .value(ReviewReplyResultMessage.SUCCESS_DELETE_REPLY.name()))
          .andExpect(
              jsonPath("$.data.message")
                  .value(ReviewReplyResultMessage.SUCCESS_DELETE_REPLY.getMessage()))
          .andExpect(jsonPath("$.data.reviewId").value(replyId))
          .andReturn();
    }

    @Test
    @DisplayName("본인의 댓글이 아닌 경우 REPLY_NOT_OWNER 예외가 발생한다.")
    void test_2() throws Exception {

      Error error = Error.of(ReviewExceptionCode.REPLY_NOT_OWNER);

      final Long reviewId = 1L;
      final Long replyId = 1L;

      mockedSecurityUtil
          .when(SecurityContextUtil::getUserIdByContext)
          .thenReturn(Optional.of(999L));
      when(reviewReplyService.deleteReviewReply(1L, 1L, 999L))
          .thenThrow(new ReviewException(ReviewExceptionCode.REPLY_NOT_OWNER));

      mockMvc
          .perform(
              delete("/api/v1/review/reply/{reviewId}/{replyId}", reviewId, replyId).with(csrf()))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
          .andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
          .andExpect(jsonPath("$.errors[0].message").value(error.message()))
          .andReturn();
    }

    @Test
    @DisplayName("존재하지 않는 댓글인 경우 NOT_FOUND_REVIEW_REPLY 예외가 발생한다.")
    void test_3() throws Exception {
      Error error = Error.of(ReviewExceptionCode.NOT_FOUND_REVIEW_REPLY);
      final Long reviewId = 1L;
      final Long replyId = 1L;

      mockedSecurityUtil.when(SecurityContextUtil::getUserIdByContext).thenReturn(Optional.of(1L));
      when(reviewReplyService.deleteReviewReply(1L, 1L, 1L))
          .thenThrow(new ReviewException(ReviewExceptionCode.NOT_FOUND_REVIEW_REPLY));

      mockMvc
          .perform(
              delete("/api/v1/review/reply/{reviewId}/{replyId}", reviewId, replyId).with(csrf()))
          .andDo(print())
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].code").value(String.valueOf(error.code())))
          .andExpect(jsonPath("$.errors[0].status").value(error.status().name()))
          .andExpect(jsonPath("$.errors[0].message").value(error.message()))
          .andReturn();
    }
  }
}
