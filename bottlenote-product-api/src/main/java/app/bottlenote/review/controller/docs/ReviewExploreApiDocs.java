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
          입력한 키워드가 담긴 리뷰를 커서 방식으로 찾아옵니다. 신규 단일 검색어는 keyword이며,
          legacy keywords는 keyword가 없을 때만 기존 AND 의미로 한시 지원됩니다. 둘을 함께 보내면 400입니다.

          sortType/sortOrder로 정렬하고 ratingFrom/ratingTo는 각 리뷰 작성 평점의 포함 하한/상한입니다.
          한쪽 경계만 보내면 이상/이하로 조회하며, 둘 다 생략하면 별점 조건을 적용하지 않습니다.
          sortType을 생략하면 LATEST(생성일시와 리뷰 ID 내림차순)를 적용합니다. 각 item의 locationInfo는 리뷰에
          저장된 위치 정보를 반환하며, 위치가 없거나 모든 위치 값이 비어 있으면 null입니다. 다음 페이지 정보와 실제 적용 조건은
          meta.pagination 및 meta.searchParameters에 담깁니다.
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
