package app.bottlenote.curation.controller.docs;

import app.bottlenote.curation.dto.response.CurationSpecListResponse;
import app.bottlenote.curation.dto.response.CurationSpecResponse;
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

/** 큐레이션 명세 조회 엔드포인트의 문서 설명. */
public final class CurationSpecApiDocs {

  private CurationSpecApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "큐레이션 명세", description = "큐레이션이 어떤 항목으로 구성되는지 정의한 명세를 조회한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "사용 중인 큐레이션 명세 목록을 조회한다",
      description =
          """
          현재 활성 상태인 큐레이션 명세를 모두 가져옵니다.

          명세는 큐레이션 화면이 어떤 항목을 어떤 형태로 담을지 정의한 틀입니다. \
          클라이언트가 화면을 구성하기 전에 이 목록으로 지원 가능한 유형을 파악합니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "활성 명세 목록",
              content =
                  @Content(
                      array =
                          @ArraySchema(
                              schema = @Schema(implementation = CurationSpecListResponse.class)))))
  public @interface GetCurationSpecs {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "큐레이션 명세 상세를 조회한다",
      description = "명세가 정의한 항목 구성과 각 항목의 형식을 반환합니다. 비활성 상태인 명세는 조회되지 않습니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "명세 상세 정보",
              content = @Content(schema = @Schema(implementation = CurationSpecResponse.class))))
  public @interface GetCurationSpec {}
}
