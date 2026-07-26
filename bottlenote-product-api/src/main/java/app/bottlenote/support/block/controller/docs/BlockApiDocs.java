package app.bottlenote.support.block.controller.docs;

import app.bottlenote.support.block.dto.response.UserBlockItem;
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

/** 사용자 차단 엔드포인트의 문서 설명. */
public final class BlockApiDocs {

  private BlockApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "차단", description = "다른 사용자를 차단하고 차단 관계를 조회한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "사용자를 차단한다",
      description =
          """
          지정한 사용자를 차단하고, 차단 후의 전체 차단 목록을 돌려줍니다.

          차단하면 상대의 리뷰와 댓글이 목록에서 보이지 않고 팔로우 관계도 정리됩니다. 차단 사유는 선택 사항입니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "차단 후의 전체 차단 목록",
              content = @Content(schema = @Schema(implementation = BlockedUserCollection.class))))
  public @interface CreateBlock {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "차단을 해제한다",
      description = "지정한 사용자의 차단을 풀고, 해제 후의 전체 차단 목록을 돌려줍니다. 차단 해제가 팔로우를 복원하지는 않습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "해제 후의 전체 차단 목록",
              content = @Content(schema = @Schema(implementation = BlockedUserCollection.class))))
  public @interface DeleteBlock {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "내가 차단한 사용자 목록을 조회한다",
      description = "내가 차단한 사용자들의 프로필과 차단 시점을 반환합니다. 설정 화면의 차단 목록에 쓰입니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "차단한 사용자 목록",
              content = @Content(schema = @Schema(implementation = BlockedUserCollection.class))))
  public @interface GetBlockedUsers {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "내가 차단한 사용자의 식별자만 조회한다",
      description = "프로필 정보 없이 식별자만 가볍게 가져옵니다. 클라이언트가 목록을 걸러낼 때 쓰는 용도입니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "차단한 사용자 식별자 목록",
              content =
                  @Content(
                      array =
                          @ArraySchema(
                              schema =
                                  @Schema(type = "integer", format = "int64", example = "42")))))
  public @interface GetBlockedUserIds {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "특정 사용자를 내가 차단했는지 확인한다",
      description = "내가 그 사용자를 차단한 상태인지 확인합니다. 상대가 나를 차단했는지는 알려주지 않습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "차단 여부",
              content =
                  @Content(
                      schema =
                          @Schema(
                              type = "boolean",
                              example = "false",
                              description = "차단했으면 true"))))
  public @interface CheckBlocked {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "특정 사용자와 서로 차단 관계인지 확인한다",
      description = "나와 상대 중 어느 쪽이라도 차단했으면 참입니다. 상호 작용이 가능한 상대인지 판단할 때 씁니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "상호 차단 여부",
              content =
                  @Content(
                      schema =
                          @Schema(
                              type = "boolean",
                              example = "false",
                              description = "어느 쪽이든 차단했으면 true"))))
  public @interface CheckMutualBlocked {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "나를 차단한 사용자 수를 조회한다",
      description = "나를 차단한 사용자의 수만 반환합니다. 누가 차단했는지는 알려주지 않습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "나를 차단한 사용자 수",
              content =
                  @Content(schema = @Schema(type = "integer", format = "int64", example = "3"))))
  public @interface GetBlockedByCount {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "내가 차단한 사용자 수를 조회한다",
      description = "내가 차단한 사용자의 수를 반환합니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "내가 차단한 사용자 수",
              content =
                  @Content(schema = @Schema(type = "integer", format = "int64", example = "5"))))
  public @interface GetBlockingCount {}

  /** 실제로는 {@code CollectionResponse<UserBlockItem>}다. */
  @Schema(name = "BlockedUserCollection", title = "차단 목록", description = "차단한 사용자 수와 목록")
  private record BlockedUserCollection(
      @Schema(description = "차단한 사용자 수", example = "5") long totalCount,
      @ArraySchema(schema = @Schema(implementation = UserBlockItem.class))
          List<UserBlockItem> items) {}
}
