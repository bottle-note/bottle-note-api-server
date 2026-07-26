package app.bottlenote.user.controller.docs;

import app.bottlenote.user.dto.response.MyBottleResponse;
import app.bottlenote.user.dto.response.MyPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 마이페이지 엔드포인트의 문서 설명. */
public final class UserMyPageApiDocs {

  private UserMyPageApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "마이페이지", description = "사용자가 남긴 리뷰·별점·찜 기록을 모아 보여준다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "마이페이지 정보를 조회한다",
      description =
          """
          대상 사용자의 프로필과 활동 요약(리뷰 수, 별점 수, 팔로워·팔로잉 수)을 반환합니다.

          다른 사용자의 마이페이지도 조회할 수 있고, 본인 페이지인지 여부가 응답에 표시됩니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "마이페이지 정보",
              content = @Content(schema = @Schema(implementation = MyPageResponse.class))))
  public @interface GetMyPage {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "리뷰를 남긴 위스키 목록을 조회한다",
      description =
          """
          대상 사용자가 리뷰를 쓴 위스키를 커서 방식으로 가져옵니다.

          다른 사용자의 목록을 볼 때는 비공개 리뷰가 제외됩니다.
          다음 페이지 정보는 meta.pageable, 조회 조건은 meta.searchParameters에 담깁니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "리뷰를 남긴 위스키 목록",
              content = @Content(schema = @Schema(implementation = MyBottleResponse.class))))
  public @interface GetReviewMyBottle {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "별점을 남긴 위스키 목록을 조회한다",
      description = "대상 사용자가 별점을 준 위스키를 커서 방식으로 가져옵니다. 각 항목에 그 사용자가 준 별점이 함께 담깁니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "별점을 남긴 위스키 목록",
              content = @Content(schema = @Schema(implementation = MyBottleResponse.class))))
  public @interface GetRatingMyBottle {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "찜한 위스키 목록을 조회한다",
      description =
          """
          대상 사용자가 찜한 위스키를 커서 방식으로 가져옵니다.

          다른 사용자의 목록을 볼 때는 각 항목에 나도 그 위스키를 찜했는지가 함께 표시됩니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "찜한 위스키 목록",
              content = @Content(schema = @Schema(implementation = MyBottleResponse.class))))
  public @interface GetPicksMyBottle {}
}
