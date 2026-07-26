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
  @Tag(name = "위스키 탐색", description = "정렬 기준을 골라 위스키를 둘러본다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "정렬 기준에 따라 위스키를 탐색한다",
      description =
          """
          인기순, 별점순, 리뷰순, 찜순, 무작위 중 원하는 기준으로 위스키를 둘러봅니다.

          무작위 정렬은 페이지를 넘길 때 중복이나 누락이 생기지 않도록 seed 값을 사용합니다. \
          첫 요청에서는 seed를 생략하면 서버가 만들어 meta.seed로 내려주고, 다음 페이지부터 그 값을 그대로 보내면 됩니다.
          다음 페이지 정보는 meta.pageable, 탐색 조건은 meta.searchParameters에 담깁니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "이번 페이지의 위스키 목록",
              content = @Content(schema = @Schema(implementation = AlcoholExploreResult.class))))
  public @interface GetStandardExplore {}

  /** 탐색 응답의 data 형태. 실제로는 {@code CollectionResponse<AlcoholDetailItem>}이다. */
  @Schema(name = "AlcoholExploreResult", title = "위스키 탐색 결과", description = "전체 건수와 이번 페이지의 위스키 목록")
  private record AlcoholExploreResult(
      @Schema(description = "전체 건수. 탐색에서는 집계하지 않아 0으로 내려간다", example = "0") long totalCount,
      @ArraySchema(schema = @Schema(implementation = AlcoholDetailItem.class))
          List<AlcoholDetailItem> items) {}
}
