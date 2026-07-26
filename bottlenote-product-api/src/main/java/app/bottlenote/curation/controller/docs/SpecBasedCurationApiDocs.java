package app.bottlenote.curation.controller.docs;

import app.bottlenote.curation.dto.response.ProductSpecBasedCurationDetailResponse;
import app.bottlenote.curation.dto.response.ProductSpecBasedCurationFeedItemResponse;
import app.bottlenote.curation.dto.response.ProductSpecBasedCurationListResponse;
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
import org.springframework.http.MediaType;

/** 큐레이션 조회 엔드포인트의 문서 설명. */
public final class SpecBasedCurationApiDocs {

  private SpecBasedCurationApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "큐레이션", description = "기획으로 엮은 위스키 모음과 피드를 조회한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "노출 중인 큐레이션 목록을 조회한다",
      description = "현재 노출 기간에 있는 큐레이션을 모두 가져옵니다. 기간이 지나거나 아직 시작하지 않은 큐레이션은 제외됩니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "노출 중인 큐레이션 목록",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      array =
                          @ArraySchema(
                              schema =
                                  @Schema(
                                      implementation =
                                          ProductSpecBasedCurationListResponse.class)))))
  public @interface GetCurations {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "큐레이션 피드를 조회한다",
      description =
          """
          큐레이션을 카드 형태로 이어 보여주는 피드를 커서 방식으로 가져옵니다.

          키워드로 제목을 검색하거나 코드로 특정 유형만 걸러낼 수 있고, 코드는 여러 개를 함께 보낼 수 있습니다.
          각 항목의 내용은 큐레이션 명세에 따라 구성되므로 유형별로 담기는 항목이 다릅니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "피드 항목과 다음 페이지 정보",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = CurationFeedPage.class))))
  public @interface GetCurationFeed {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "큐레이션 상세를 조회한다",
      description = "큐레이션의 소개 정보와 편성된 위스키를 반환합니다. 삭제된 위스키는 제외되고, 노출 기간이 아닌 큐레이션은 조회되지 않습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "큐레이션 상세 정보",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema =
                          @Schema(implementation = ProductSpecBasedCurationDetailResponse.class))))
  public @interface GetCuration {}

  /** 실제로는 {@code CursorResponse<ProductSpecBasedCurationFeedItemResponse>}다. */
  @Schema(
      name = "CurationFeedPage",
      title = "큐레이션 피드 페이지",
      description = "이번 페이지의 피드 항목과 다음 페이지 정보")
  private record CurationFeedPage(
      @ArraySchema(
              schema = @Schema(implementation = ProductSpecBasedCurationFeedItemResponse.class))
          List<ProductSpecBasedCurationFeedItemResponse> items,
      CursorPageable pageable) {}
}
