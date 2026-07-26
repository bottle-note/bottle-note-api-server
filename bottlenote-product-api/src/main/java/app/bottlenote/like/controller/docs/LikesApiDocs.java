package app.bottlenote.like.controller.docs;

import app.bottlenote.like.dto.response.LikesUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.http.MediaType;

/** 리뷰 좋아요 엔드포인트의 문서 설명. */
public final class LikesApiDocs {

  private LikesApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "좋아요", description = "리뷰에 좋아요를 누르거나 취소한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "리뷰 좋아요 상태를 변경한다",
      description =
          """
          리뷰에 좋아요를 누르거나 취소합니다.

          같은 요청을 반복해도 결과가 같도록 원하는 상태를 직접 지정합니다. 본인이 쓴 리뷰에도 누를 수 있습니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "변경된 좋아요 상태",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = LikesUpdateResponse.class))))
  public @interface UpdateLikes {}
}
