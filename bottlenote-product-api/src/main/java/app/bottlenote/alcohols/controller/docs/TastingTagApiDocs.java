package app.bottlenote.alcohols.controller.docs;

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

/** 테이스팅 태그 추출 엔드포인트의 문서 설명. */
public final class TastingTagApiDocs {

  private TastingTagApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "테이스팅 태그", description = "리뷰 문장에서 맛과 향 표현을 뽑아낸다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "문장에서 테이스팅 태그를 추출한다",
      description =
          """
          리뷰를 쓰는 중에 입력한 문장을 보내면 그 안에 담긴 맛과 향 표현을 태그로 뽑아 돌려줍니다.

          리뷰 작성 화면에서 태그를 자동으로 제안하기 위한 용도입니다. 추출된 태그가 곧바로 리뷰에 저장되지는 않습니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "추출된 태그 이름 목록",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      array =
                          @ArraySchema(
                              schema =
                                  @Schema(
                                      type = "string",
                                      example = "달달한",
                                      description = "태그 이름")))))
  public @interface ExtractTags {}
}
