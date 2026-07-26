package app.bottlenote.user.controller.docs;

import app.bottlenote.user.dto.response.FollowUpdateResponse;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
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

/** 팔로우 엔드포인트의 문서 설명. */
public final class FollowApiDocs {

  private FollowApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "팔로우", description = "다른 사용자를 팔로우하고 목록을 조회한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "특정 사용자가 팔로우하는 사람 목록을 조회한다",
      description =
          """
          대상 사용자가 팔로우하고 있는 사람들을 커서 방식으로 가져옵니다.

          각 항목에는 내가 그 사람을 팔로우하고 있는지도 함께 표시됩니다. 다음 페이지 정보는 meta.pageable에 담깁니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "팔로잉 목록",
              content =
                  @Content(
                      array =
                          @ArraySchema(
                              schema = @Schema(implementation = FollowingSearchResponse.class)))))
  public @interface GetFollowingList {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "특정 사용자를 팔로우하는 사람 목록을 조회한다",
      description = "대상 사용자를 팔로우하는 사람들을 커서 방식으로 가져옵니다. 각 항목에 내가 그 사람을 팔로우하는지 함께 표시됩니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "팔로워 목록",
              content =
                  @Content(
                      array =
                          @ArraySchema(
                              schema = @Schema(implementation = FollowerSearchResponse.class)))))
  public @interface GetFollowerList {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "팔로우 상태를 변경한다",
      description = "대상 사용자를 팔로우하거나 팔로우를 취소합니다. 자기 자신은 팔로우할 수 없고, 차단한 사용자와는 팔로우가 성립하지 않습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "변경된 팔로우 상태",
              content = @Content(schema = @Schema(implementation = FollowUpdateResponse.class))))
  public @interface UpdateFollowStatus {}
}
