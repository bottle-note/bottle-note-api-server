package app.bottlenote.review.controller.docs;

import app.bottlenote.review.dto.response.ReviewExploreItem;
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
import java.util.List;

/** 리뷰 탐색 엔드포인트의 문서 설명. */
public final class ReviewExploreApiDocs {

  private ReviewExploreApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "둘러보기", description = "원하는 기준으로 콘텐츠를 둘러본다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "키워드로 리뷰를 탐색한다",
      description =
          """
          입력한 키워드가 담긴 리뷰를 커서 방식으로 찾아옵니다.

          키워드를 여러 개 보내면 그중 하나라도 걸리는 리뷰를 가져오고, 키워드를 생략하면 전체 리뷰를 대상으로 합니다.
          각 item의 locationInfo는 리뷰에 저장된 위치 정보를 반환하며, 위치가 없거나 모든 위치 값이 비어 있으면 null입니다.
          다음 페이지 정보는 meta.pagination, 검색에 사용한 키워드는 meta.searchParameters에 담깁니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "이번 페이지의 리뷰 목록",
              content = @Content(schema = @Schema(implementation = ReviewExploreResult.class))))
  public @interface GetStandardExplore {}

  @Schema(name = "ReviewExploreResult", title = "리뷰 탐색 결과", description = "이번 페이지의 리뷰 목록")
  private record ReviewExploreResult(
      @ArraySchema(schema = @Schema(implementation = ReviewExploreItem.class))
          List<ReviewExploreItem> items) {}
}
