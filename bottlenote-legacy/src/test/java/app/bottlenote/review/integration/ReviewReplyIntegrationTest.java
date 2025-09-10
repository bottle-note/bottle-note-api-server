package app.bottlenote.review.integration;

import static app.bottlenote.review.constant.ReviewReplyStatus.DELETED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.review.constant.ReviewReplyResultMessage;
import app.bottlenote.review.constant.ReviewReplyStatus;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.domain.ReviewReplyRepository;
import app.bottlenote.review.dto.request.ReviewReplyRegisterRequest;
import app.bottlenote.review.dto.response.ReviewReplyResponse;
import app.bottlenote.review.dto.response.RootReviewReplyResponse;
import app.bottlenote.review.dto.response.SubReviewReplyResponse;
import app.bottlenote.shared.data.response.GlobalResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@DisplayName("[integration] [controller] ReviewReplyController")
class ReviewReplyIntegrationTest extends IntegrationTestSupport {

  @Autowired private ReviewReplyRepository reviewReplyRepository;

  @Nested
  @DisplayName("리뷰 댓글 생성 테스트")
  class create {

    @DisplayName("리뷰의 댓글을 생성할 수 있다.")
    @Sql(
        scripts = {
          "/init-script/init-alcohol.sql",
          "/init-script/init-user.sql",
          "/init-script/init-review.sql",
          "/init-script/init-review-reply.sql"
        })
    @Test
    void test_1() throws Exception {

      ReviewReplyRegisterRequest replyRegisterRequest =
          new ReviewReplyRegisterRequest("댓글 내용", null);
      final Long reviewId = 1L;

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/v1/review/reply/register/{reviewId}", reviewId)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(mapper.writeValueAsString(replyRegisterRequest))
                      .header("Authorization", "Bearer " + getToken())
                      .with(csrf()))
              .andDo(print())
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.data").exists())
              .andReturn();

      String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
      GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
      ReviewReplyResponse reviewReplyResponse =
          mapper.convertValue(response.getData(), ReviewReplyResponse.class);

      assertEquals(
          ReviewReplyResultMessage.SUCCESS_REGISTER_REPLY, reviewReplyResponse.codeMessage());
    }

    @DisplayName("댓글의 대댓글을 생성할 수 있다.")
    @Sql(
        scripts = {
          "/init-script/init-alcohol.sql",
          "/init-script/init-user.sql",
          "/init-script/init-review.sql"
        })
    @Test
    void test_2() throws Exception {

      final Long reviewId = 1L;
      ReviewReply savedReply =
          reviewReplyRepository.save(
              ReviewReply.builder()
                  .reviewId(reviewId)
                  .userId(getTokenUserId())
                  .content("댓글 내용")
                  .status(ReviewReplyStatus.NORMAL)
                  .build());

      ReviewReplyRegisterRequest replyRegisterRequest =
          new ReviewReplyRegisterRequest("대댓글 내용", savedReply.getId());

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/v1/review/reply/register/{reviewId}", reviewId)
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(mapper.writeValueAsString(replyRegisterRequest))
                      .header("Authorization", "Bearer " + getToken())
                      .with(csrf()))
              .andDo(print())
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.data").exists())
              .andReturn();

      String contentAsString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
      GlobalResponse response = mapper.readValue(contentAsString, GlobalResponse.class);
      ReviewReplyResponse reviewReplyResponse =
          mapper.convertValue(response.getData(), ReviewReplyResponse.class);

      assertEquals(
          ReviewReplyResultMessage.SUCCESS_REGISTER_REPLY, reviewReplyResponse.codeMessage());
    }
  }

  @Nested
  @DisplayName("리뷰 댓글 조회 테스트")
  class read {

    @DisplayName("리뷰의 최상위 댓글을 조회할 수 있다.")
    @Sql(
        scripts = {
          "/init-script/init-alcohol.sql",
          "/init-script/init-user.sql",
          "/init-script/init-review.sql",
          "/init-script/init-review-reply.sql"
        })
    @Test
    void test_1() throws Exception {

      // given
      final Long reviewId = 4L;
      // when && then
      MvcResult result =
          mockMvc
              .perform(
                  get("/api/v1/review/reply/{reviewId}", reviewId)
                      .contentType(MediaType.APPLICATION_JSON)
                      .param("cursor", "0")
                      .param("pageSize", "50")
                      .with(csrf()))
              .andDo(print())
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.data").exists())
              .andExpect(jsonPath("$.data.length()").value(2))
              .andReturn();

      String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
      log.info("responseString : {}", responseString);
    }

    @DisplayName("리뷰의 대댓글 목록을 조회할 수 있다.")
    @Sql(
        scripts = {
          "/init-script/init-alcohol.sql",
          "/init-script/init-user.sql",
          "/init-script/init-review.sql"
        })
    @Test
    void test_2() throws Exception {

      final Long reviewId = 1L;
      ReviewReply savedReply =
          reviewReplyRepository.save(
              ReviewReply.builder()
                  .reviewId(reviewId)
                  .userId(getTokenUserId())
                  .content("댓글 내용")
                  .status(ReviewReplyStatus.NORMAL)
                  .build());

      ReviewReplyRegisterRequest replyRegisterRequest =
          new ReviewReplyRegisterRequest("대댓글 내용", savedReply.getId());
      final int count = 2;

      for (int i = 0; i < count; i++) {
        mockMvc
            .perform(
                post("/api/v1/review/reply/register/{reviewId}", reviewId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(replyRegisterRequest))
                    .header("Authorization", "Bearer " + getToken())
                    .with(csrf()))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data").exists());
      }

      MvcResult result =
          mockMvc
              .perform(
                  get(
                          "/api/v1/review/reply/{reviewId}/sub/{rootReplyId}",
                          reviewId,
                          savedReply.getId())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(mapper.writeValueAsString(replyRegisterRequest))
                      .header("Authorization", "Bearer " + getToken())
                      .with(csrf()))
              .andDo(print())
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.data").exists())
              .andReturn();

      String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
      GlobalResponse globalResponse = mapper.readValue(responseString, GlobalResponse.class);
      SubReviewReplyResponse subReviewReplyResponse =
          mapper.convertValue(globalResponse.getData(), SubReviewReplyResponse.class);

      assertEquals(count, subReviewReplyResponse.totalCount());
    }
  }

  @Nested
  @DisplayName("리뷰 댓글 삭제 테스트")
  class delete {

    @DisplayName("리뷰 댓글을 삭제할 수 있다.")
    @Sql(
        scripts = {
          "/init-script/init-alcohol.sql",
          "/init-script/init-user.sql",
          "/init-script/init-review.sql"
        })
    @Test
    void test_1() throws Exception {

      final Long reviewId = 1L;

      ReviewReply savedReply =
          reviewReplyRepository.save(
              ReviewReply.builder()
                  .reviewId(reviewId)
                  .userId(getTokenUserId())
                  .content("댓글 내용")
                  .status(ReviewReplyStatus.NORMAL)
                  .build());

      mockMvc
          .perform(
              delete("/api/v1/review/reply/{reviewId}/{replyId}", reviewId, savedReply.getId())
                  .contentType(MediaType.APPLICATION_JSON)
                  .header("Authorization", "Bearer " + getToken())
                  .with(csrf()))
          .andDo(print())
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(200))
          .andExpect(jsonPath("$.data").exists());

      MvcResult result =
          mockMvc
              .perform(
                  get("/api/v1/review/reply/{reviewId}", reviewId)
                      .contentType(MediaType.APPLICATION_JSON)
                      .with(csrf()))
              .andDo(print())
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200))
              .andExpect(jsonPath("$.data").exists())
              .andExpect(jsonPath("$.data.length()").value(2))
              .andReturn();

      String responseString = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
      GlobalResponse globalResponse = mapper.readValue(responseString, GlobalResponse.class);
      RootReviewReplyResponse rootReviewReplyResponse =
          mapper.convertValue(globalResponse.getData(), RootReviewReplyResponse.class);

      assertEquals(
          rootReviewReplyResponse.reviewReplies().get(0).reviewReplyContent(),
          DELETED.getMessage());
    }
  }
}
