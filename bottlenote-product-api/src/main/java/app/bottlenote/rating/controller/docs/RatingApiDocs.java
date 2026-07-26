package app.bottlenote.rating.controller.docs;

import app.bottlenote.rating.dto.response.RatingListFetchResponse;
import app.bottlenote.rating.dto.response.RatingRegisterResponse;
import app.bottlenote.rating.dto.response.UserRatingResponse;
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

/** 별점 엔드포인트의 문서 설명. */
public final class RatingApiDocs {

  private RatingApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "별점", description = "위스키에 별점을 주고 조회한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "위스키에 별점을 준다",
      description =
          """
          위스키에 0.5 단위의 별점을 남깁니다.

          이미 별점을 준 위스키에 다시 요청하면 기존 별점을 덮어씁니다. 별점은 리뷰와 별개로 관리되므로 리뷰 없이도 줄 수 있습니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "등록된 별점 정보",
              content = @Content(schema = @Schema(implementation = RatingRegisterResponse.class))))
  public @interface RegisterRating {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "별점을 매길 위스키 목록을 조회한다",
      description =
          """
          아직 별점을 주지 않은 위스키를 커서 방식으로 가져옵니다.

          별점 매기기 화면에서 다음에 평가할 대상을 보여주기 위한 목록입니다.
          다음 페이지 정보는 meta.pageable, 조회 조건은 meta.searchParameters에 담깁니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "별점을 매기지 않은 위스키 목록",
              content =
                  @Content(
                      array =
                          @ArraySchema(
                              schema = @Schema(implementation = RatingListFetchResponse.class)))))
  public @interface FetchRatingList {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "특정 위스키에 내가 준 별점을 조회한다",
      description = "로그인한 사용자가 그 위스키에 준 별점을 반환합니다. 아직 별점을 주지 않았으면 값이 비어 있습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "내가 준 별점",
              content = @Content(schema = @Schema(implementation = UserRatingResponse.class))))
  public @interface FetchUserRating {}
}
