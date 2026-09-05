package app.bottlenote.alcohols.controller.docs;

import app.bottlenote.alcohols.dto.response.AlcoholDetailItem;
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

/** 위스키 탐색 엔드포인트의 문서 설명. */
public final class AlcoholExploreApiDocs {

  private AlcoholExploreApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "둘러보기", description = "원하는 기준으로 콘텐츠를 둘러본다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "정렬 기준에 따라 위스키를 탐색한다",
      description =
          """
          인기순, 별점순, 리뷰순, 찜순, 무작위 중 원하는 기준으로 위스키를 둘러봅니다.
          동일한 검색 조건에서는 정렬 방식과 관계없이 검색 대상이 같습니다.
          인기 점수가 없는 위스키도 포함하며, 정렬할 때는 0점으로 취급합니다.
          인기순의 점수 기준은 첫 페이지에서 고정되며, 점수가 같으면 ID 오름차순으로 정렬합니다.

          ratingFrom/ratingTo는 목록에 표시되는 0.5 단위 반올림 집계 평점의 포함 하한/상한입니다.
          한쪽 경계만 보내면 이상/이하로 조회하며, 둘 다 생략하면 별점 조건을 적용하지 않습니다.
          무작위 정렬의 시드는 HMAC 커서 extra에 담기며, 다음 페이지는 meta.pagination.nextCursor를 그대로 보내면 됩니다.
          탐색 조건은 meta.searchParameters에 담깁니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "이번 페이지의 위스키 목록",
              content = @Content(schema = @Schema(implementation = AlcoholExploreResult.class))))
  public @interface GetStandardExplore {}

  /** 탐색 응답의 data 형태. 실제로는 {@code ExploreStandardResponse}다. */
  @Schema(name = "AlcoholExploreResult", title = "위스키 탐색 결과", description = "이번 페이지의 위스키 목록")
  private record AlcoholExploreResult(
      @ArraySchema(schema = @Schema(implementation = AlcoholDetailItem.class))
          List<AlcoholDetailItem> items) {}
}
