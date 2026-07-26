package app.bottlenote.banner.controller.docs;

import app.bottlenote.banner.dto.response.BannerResponse;
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

/** 배너 조회 엔드포인트의 문서 설명. */
public final class BannerApiDocs {

  private BannerApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "배너", description = "앱 화면에 노출할 배너를 조회한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "노출 중인 배너를 조회한다",
      description =
          """
          현재 노출 기간에 있는 배너를 우선순위 순으로 가져옵니다.

          limit으로 받아올 개수를 조절할 수 있습니다. 기간이 지났거나 비활성 상태인 배너는 제외됩니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "노출 중인 배너 목록",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      array =
                          @ArraySchema(schema = @Schema(implementation = BannerResponse.class)))))
  public @interface GetActiveBanners {}
}
