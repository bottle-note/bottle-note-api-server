package app.bottlenote.alcohols.controller.docs;

import app.bottlenote.alcohols.dto.response.PopularItem;
import app.bottlenote.alcohols.dto.response.PopularsOfWeekResponse;
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

/** 인기 위스키 엔드포인트의 문서 설명. */
public final class AlcoholPopularApiDocs {

  private AlcoholPopularApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "인기 위스키", description = "기간과 기준별로 집계한 인기 위스키를 제공한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "이번 주 인기 위스키를 조회한다",
      description = "최근 한 주간의 별점과 리뷰 활동을 합산해 순위를 매깁니다. top 값으로 받아올 개수를 정할 수 있습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "전체 건수와 인기 위스키 목록",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = PopularsOfWeekResponse.class))))
  public @interface GetPopularOfWeek {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "봄 추천 위스키를 조회한다",
      description = "계절 기획으로 선정한 봄 추천 위스키 목록입니다. 집계 결과가 아니라 미리 고른 목록이라 요청 조건이 없습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "봄 추천 위스키 목록",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      array = @ArraySchema(schema = @Schema(implementation = PopularItem.class)))))
  public @interface GetSpringItems {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "이번 주 조회수 기준 인기 위스키를 조회한다",
      description = "최근 한 주간 상세 페이지를 많이 본 순서로 순위를 매깁니다. 별점이나 리뷰가 아니라 조회 이력만 봅니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "전체 건수와 조회수 상위 위스키 목록",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = PopularsOfWeekResponse.class))))
  public @interface GetPopularByViewsWeekly {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "이번 달 조회수 기준 인기 위스키를 조회한다",
      description = "최근 한 달간 상세 페이지를 많이 본 순서로 순위를 매깁니다. 주간 집계보다 순위가 천천히 바뀝니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "전체 건수와 조회수 상위 위스키 목록",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = PopularsOfWeekResponse.class))))
  public @interface GetPopularByViewsMonthly {}
}
