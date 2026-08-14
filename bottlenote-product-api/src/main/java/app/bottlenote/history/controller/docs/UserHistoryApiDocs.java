package app.bottlenote.history.controller.docs;

import app.bottlenote.history.dto.response.UserHistorySearchResponse;
import app.bottlenote.history.dto.response.ViewHistoryListResponse;
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

          기간이나 활동 유형으로 걸러낼 수 있습니다. 다음 페이지 정보는 meta.pagination에 담깁니다.
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
      description =
          """
          로그인한 사용자가 최근에 상세 페이지를 열어본 위스키를 최신순으로 가져옵니다.

          요청은 cursor와 size(기본 6)입니다. 다음 페이지 정보는 meta.pagination에 담깁니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "최근 본 위스키 목록",
              content = @Content(schema = @Schema(implementation = ViewHistoryListResponse.class))))
  public @interface GetViewHistory {}
}
