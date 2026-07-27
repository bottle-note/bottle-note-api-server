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

          **요청 값 오류 코드**

          | 코드 | 상태 코드 | 발생 조건 | 설명 |
          | --- | --- | --- | --- |
          | `REVIEW_ID_REQUIRED` | 400 | alcoholId를 보내지 않았을 때 | reviewId(식별자)는 필수입니다. |
          | `REVIEW_ID_MINIMUM` | 400 | alcoholId가 1 미만일 때 | 리뷰 식별자는 최소 1 이상이어야 합니다. |
          | `REVIEW_CONTENT_REQUIRED` | 400 | content가 비어 있을 때 | 리뷰 내용은 필수입니다. |
          | `REVIEW_CONTENT_MAXIMUM` | 400 | content가 700자를 넘을 때 | 리뷰 내용의 최대 글자수를 초과했습니다. |
          | `PRICE_MINIMUM` | 400 | price가 0 미만일 때 | 가격은 0원 이상이어야 합니다. |
          | `PRICE_MAXIMUM` | 400 | price가 1조를 넘을 때 | 입력할 수 있는 가격의 범위가 아닙니다. |
          | `INVALID_ZIP_CODE_PATTERN` | 400 | locationInfo.zipCode가 숫자 5자리가 아닐 때 | 우편번호는 숫자 5자리 형식입니다. |
          | `REVIEW_IMAGE_ORDER_REQUIRED` | 400 | imageUrlList 항목에 order가 없을 때 | 리뷰 이미지 Order 값은 필수입니다 |
          | `REVIEW_IMAGE_URL_REQUIRED` | 400 | imageUrlList 항목에 viewUrl이 없을 때 | 리뷰 이미지 URL은 필수입니다 |

          **처리 중 오류 코드**

          | 코드 | 상태 코드 | 발생 조건 | 설명 |
          | --- | --- | --- | --- |
          | `REQUIRED_USER_ID` | 400 | 액세스 토큰에서 사용자 식별자를 얻지 못했을 때 | 유저 아이디가 필요합니다. |
          | `ALCOHOL_NOT_FOUND` | 404 | alcoholId에 해당하는 위스키가 없을 때 | 위스키를 찾을 수 없습니다. |
          | `USER_NOT_FOUND` | 404 | 토큰이 가리키는 사용자가 없을 때 | 유저를 찾을 수 없습니다. |
          | `INVALID_RESOURCE_URL` | 400 | 이미지 주소에서 리소스 키를 뽑지 못했을 때 | 유효하지 않은 리소스 URL입니다. |
          | `RESOURCE_NOT_FOUND` | 400 | 업로드 기록이 없는 이미지일 때 | 등록되지 않은 리소스입니다. |
          | `RESOURCE_OWNER_MISMATCH` | 400 | 다른 사용자가 올린 이미지일 때 | 리소스 소유자가 일치하지 않습니다. |
          | `RESOURCE_ALREADY_USED` | 400 | 이미 다른 곳에 쓰인 이미지일 때 | 사용할 수 없는 리소스 상태입니다. |
          | `INVALID_RATING_POINT` | 400 | rating이 0.0~5.0의 0.5 단위 값이 아닐 때 | 평점은 0.0/1.0/1.5/2.0/2.5/3.0/3.5/4.0/4.5/5.0 중 하나의 값을 가질 수 있습니다. |
          | `INVALID_IMAGE_URL_MAX_SIZE` | 400 | 이미지가 5장을 넘을 때 | 이미지는 최대 5장까지만 업로드 할 수 있습니다 |
          | `INVALID_TASTING_TAG_LIST_SIZE` | 400 | 테이스팅 태그가 15개를 넘을 때 | 테이스팅 태그는 15개까지만 작성할 수 있습니다. |
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "작성된 리뷰의 식별자",
              content = @Content(schema = @Schema(implementation = ReviewCreateResponse.class))))
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
              content = @Content(schema = @Schema(implementation = ReviewDetailResponse.class))))
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
              content = @Content(schema = @Schema(implementation = ReviewResultResponse.class))))
  public @interface ModifyReview {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "리뷰의 공개 여부를 변경한다",
      description =
          """
          공개로 바꾸면 다른 사용자에게 보이고, 비공개로 바꾸면 작성자 본인에게만 보입니다. 본인이 작성한 리뷰만 변경할 수 있습니다.

          **오류 코드**

          | 코드 | 상태 코드 | 발생 조건 | 설명 |
          | --- | --- | --- | --- |
          | `REVIEW_DISPLAY_STATUS_NOT_EMPTY` | 400 | status를 보내지 않았을 때 | 리뷰 공개/비공개상태는 필수입니다. |
          | `REQUIRED_USER_ID` | 400 | 액세스 토큰에서 사용자 식별자를 얻지 못했을 때 | 유저 아이디가 필요합니다. |
          | `REVIEW_NOT_FOUND` | 400 | 리뷰가 없거나 본인이 쓴 리뷰가 아닐 때 | 리뷰를 찾을 수 없습니다 |
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "변경 처리 결과",
              content = @Content(schema = @Schema(implementation = ReviewResultResponse.class))))
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
              content = @Content(schema = @Schema(implementation = ReviewResultResponse.class))))
  public @interface DeleteReview {}
}
