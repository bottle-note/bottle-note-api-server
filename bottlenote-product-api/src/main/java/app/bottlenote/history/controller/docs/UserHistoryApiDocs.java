package app.bottlenote.history.controller.docs;

import app.bottlenote.alcohols.dto.response.ViewHistoryItem;
import app.bottlenote.history.dto.response.UserHistorySearchResponse;
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

/** 활동 기록 엔드포인트의 문서 설명. */
public final class UserHistoryApiDocs {

  private UserHistoryApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "활동 기록", description = "사용자의 활동 내역과 최근 본 위스키를 조회한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "사용자의 활동 기록을 조회한다",
      description =
          """
          대상 사용자가 남긴 리뷰·별점·찜 등의 활동을 시간순으로 커서 방식으로 가져옵니다.

          기간이나 활동 유형으로 걸러낼 수 있습니다. 다음 페이지 정보는 meta.pageable에 담깁니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "활동 기록 목록",
              content =
                  @Content(
                      array =
                          @ArraySchema(
                              schema = @Schema(implementation = UserHistorySearchResponse.class)))))
  public @interface GetUserHistoryList {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "최근 본 위스키를 조회한다",
      description = "로그인한 사용자가 최근에 상세 페이지를 열어본 위스키를 최신순으로 반환합니다. 조회 이력은 일정 주기로 집계됩니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "전체 건수와 최근 본 위스키 목록",
              content = @Content(schema = @Schema(implementation = ViewHistoryCollection.class))))
  public @interface GetViewHistory {}

  /** 실제로는 {@code CollectionResponse<ViewHistoryItem>}다. */
  @Schema(name = "ViewHistoryCollection", title = "최근 본 위스키 목록", description = "전체 건수와 최근 본 위스키")
  private record ViewHistoryCollection(
      @Schema(description = "최근 본 위스키 건수", example = "12") long totalCount,
      @ArraySchema(schema = @Schema(implementation = ViewHistoryItem.class))
          List<ViewHistoryItem> items) {}
}
