package app.bottlenote.review.controller.docs;

import app.bottlenote.review.dto.response.ReviewReplyResponse;
import app.bottlenote.review.dto.response.RootReviewReplyResponse;
import app.bottlenote.review.dto.response.SubReviewReplyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 리뷰 댓글 엔드포인트의 문서 설명. */
public final class ReviewReplyApiDocs {

  private ReviewReplyApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "리뷰 댓글", description = "리뷰에 달리는 댓글과 대댓글을 관리한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "리뷰에 댓글을 등록한다",
      description =
          """
          리뷰에 댓글을 남깁니다.

          다른 댓글의 식별자를 함께 보내면 그 댓글에 대한 대댓글로 등록됩니다.
          대댓글의 대댓글은 지원하지 않으므로 최상위 댓글의 식별자만 지정할 수 있습니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "등록된 댓글 정보",
              content = @Content(schema = @Schema(implementation = ReviewReplyResponse.class))))
  public @interface RegisterReply {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "댓글을 삭제한다",
      description = "본인이 작성한 댓글만 삭제할 수 있습니다. 대댓글이 달린 댓글을 삭제하면 삭제된 댓글로 표시되고 대댓글은 남습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "삭제 처리 결과",
              content = @Content(schema = @Schema(implementation = ReviewReplyResponse.class))))
  public @interface DeleteReply {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "리뷰의 댓글 목록을 조회한다",
      description =
          "대댓글을 제외한 최상위 댓글만 커서 방식으로 가져옵니다. 각 댓글에 달린 대댓글 수가 함께 표시됩니다. 로그인하지 않아도 조회할 수 있습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "최상위 댓글 목록",
              content = @Content(schema = @Schema(implementation = RootReviewReplyResponse.class))))
  public @interface GetRootReplies {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "댓글에 달린 대댓글 목록을 조회한다",
      description = "지정한 최상위 댓글에 달린 대댓글만 커서 방식으로 가져옵니다. 로그인하지 않아도 조회할 수 있습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "대댓글 목록",
              content = @Content(schema = @Schema(implementation = SubReviewReplyResponse.class))))
  public @interface GetSubReplies {}
}
