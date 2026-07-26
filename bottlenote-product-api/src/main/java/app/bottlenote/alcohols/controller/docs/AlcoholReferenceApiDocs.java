package app.bottlenote.alcohols.controller.docs;

import app.bottlenote.alcohols.dto.response.AlcoholsSearchItem;
import app.bottlenote.alcohols.dto.response.CategoryItem;
import app.bottlenote.alcohols.dto.response.CurationKeywordResponse;
import app.bottlenote.alcohols.dto.response.RegionsItem;
import app.bottlenote.global.service.cursor.CursorPageable;
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

/** 위스키 기준 정보와 큐레이션 조회 엔드포인트의 문서 설명. */
public final class AlcoholReferenceApiDocs {

  private AlcoholReferenceApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "위스키 기준 정보", description = "지역과 종류 같은 선택 목록, 그리고 큐레이션을 조회한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "위스키 생산 지역 목록을 조회한다",
      description = "검색 필터에서 지역을 고를 때 쓰는 전체 목록입니다. 자주 바뀌지 않으므로 클라이언트에서 캐싱해도 좋습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "지역 목록",
              content =
                  @Content(
                      array = @ArraySchema(schema = @Schema(implementation = RegionsItem.class)))))
  public @interface GetRegions {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "위스키 종류 목록을 조회한다",
      description = "검색 필터에서 종류를 고를 때 쓰는 목록입니다. 종류를 지정하면 그 종류에 속한 세부 분류만 가져옵니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "종류 목록",
              content =
                  @Content(
                      array = @ArraySchema(schema = @Schema(implementation = CategoryItem.class)))))
  public @interface GetCategories {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "큐레이션 키워드를 검색한다",
      description = "노출 중인 큐레이션을 커서 방식으로 가져옵니다. 응답의 pageable로 다음 페이지를 이어 요청합니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "큐레이션 키워드 목록",
              content = @Content(schema = @Schema(implementation = CurationKeywordPage.class))))
  public @interface SearchCurationKeywords {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "큐레이션에 담긴 위스키를 조회한다",
      description = "해당 큐레이션에 편성된 위스키를 커서 방식으로 가져옵니다. 삭제된 위스키는 제외됩니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "큐레이션에 담긴 위스키 목록",
              content = @Content(schema = @Schema(implementation = CurationAlcoholPage.class))))
  public @interface GetCurationAlcohols {}

  /** 실제로는 {@code CursorResponse<CurationKeywordResponse>}다. */
  @Schema(
      name = "CurationKeywordPage",
      title = "큐레이션 키워드 페이지",
      description = "이번 페이지의 큐레이션과 다음 페이지 정보")
  private record CurationKeywordPage(
      @ArraySchema(schema = @Schema(implementation = CurationKeywordResponse.class))
          List<CurationKeywordResponse> items,
      CursorPageable pageable) {}

  /** 실제로는 {@code CursorResponse<AlcoholsSearchItem>}다. */
  @Schema(
      name = "CurationAlcoholPage",
      title = "큐레이션 위스키 페이지",
      description = "이번 페이지의 위스키와 다음 페이지 정보")
  private record CurationAlcoholPage(
      @ArraySchema(schema = @Schema(implementation = AlcoholsSearchItem.class))
          List<AlcoholsSearchItem> items,
      CursorPageable pageable) {}
}
