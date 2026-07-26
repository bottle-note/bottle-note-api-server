package app.bottlenote.review.controller.docs;

import app.bottlenote.review.dto.response.ReviewCreateResponse;
import app.bottlenote.review.dto.response.ReviewDetailResponse;
import app.bottlenote.review.dto.response.ReviewListResponse;
import app.bottlenote.review.dto.response.ReviewResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.MediaType;

/**
 * 리뷰 엔드포인트의 문서 설명을 모아둔다.
 *
 * <p>컨트롤러 본문에는 어노테이션 한 줄만 붙어 문서와 코드가 서로를 가리지 않게 한다. 여기 적는 문장은 API 문서 화면에 그대로 노출되므로 식별자가 아닌 사람이 읽는
 * 문장으로 쓴다.
 */
public final class ReviewApiDocs {

  private ReviewApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "리뷰", description = "위스키에 대한 리뷰를 작성하고 조회하고 수정한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "리뷰를 작성한다",
      description =
          """
          특정 위스키에 대한 리뷰를 남깁니다.

          별점은 리뷰와 별개로 관리되므로 이 요청만으로는 별점이 등록되지 않습니다.
          비공개로 작성하면 목록과 상세 조회에서 작성자 본인에게만 보입니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "작성된 리뷰의 식별자",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = ReviewCreateResponse.class))))
  public @interface CreateReview {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "위스키의 리뷰 목록을 조회한다",
      description =
          """
          해당 위스키에 달린 리뷰를 커서 방식으로 나눠 가져옵니다.

          로그인하지 않아도 조회할 수 있습니다. 로그인한 경우에는 각 리뷰에 본인이 좋아요를 눌렀는지가 함께 표시됩니다.
          다음 페이지 정보는 응답의 meta.pageable에 담깁니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "리뷰 목록",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      array =
                          @ArraySchema(
                              schema = @Schema(implementation = ReviewListResponse.class)))))
  public @interface GetReviews {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "리뷰 하나를 상세히 조회한다",
      description = "리뷰 본문과 작성자 정보, 좋아요와 댓글 수를 함께 반환합니다. 로그인하지 않아도 조회할 수 있습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "리뷰 상세 정보",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = ReviewDetailResponse.class))))
  public @interface GetReviewDetail {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "내가 쓴 리뷰 목록을 조회한다",
      description = "해당 위스키에 내가 남긴 리뷰만 커서 방식으로 가져옵니다. 비공개로 작성한 리뷰도 포함됩니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "내가 작성한 리뷰 목록",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      array =
                          @ArraySchema(
                              schema = @Schema(implementation = ReviewListResponse.class)))))
  public @interface GetMyReviews {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "리뷰를 수정한다",
      description = "본인이 작성한 리뷰만 수정할 수 있습니다. 본문, 가격 정보, 함께한 자리 정보를 바꿀 수 있습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "수정 처리 결과",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = ReviewResultResponse.class))))
  public @interface ModifyReview {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "리뷰의 공개 여부를 변경한다",
      description = "공개로 바꾸면 다른 사용자에게 보이고, 비공개로 바꾸면 작성자 본인에게만 보입니다. 본인이 작성한 리뷰만 변경할 수 있습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "변경 처리 결과",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = ReviewResultResponse.class))))
  public @interface ChangeReviewStatus {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "리뷰를 삭제한다",
      description = "본인이 작성한 리뷰만 삭제할 수 있습니다. 삭제한 리뷰는 목록과 상세 조회에서 더 이상 보이지 않습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "삭제 처리 결과",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = ReviewResultResponse.class))))
  public @interface DeleteReview {}
}
