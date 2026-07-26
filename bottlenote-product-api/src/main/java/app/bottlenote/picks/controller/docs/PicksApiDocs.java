package app.bottlenote.picks.controller.docs;

import app.bottlenote.picks.dto.response.PicksUpdateResponse;
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

/** 찜하기 엔드포인트의 문서 설명. */
public final class PicksApiDocs {

  private PicksApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "찜하기", description = "관심 있는 위스키를 찜하거나 해제한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "위스키 찜 상태를 변경한다",
      description =
          """
          위스키를 찜하거나 찜을 해제합니다.

          같은 요청을 반복해도 결과가 같도록 원하는 상태를 직접 지정합니다. 찜한 위스키는 마이페이지에서 모아 볼 수 있습니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "변경된 찜 상태",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = PicksUpdateResponse.class))))
  public @interface UpdatePicks {}
}
