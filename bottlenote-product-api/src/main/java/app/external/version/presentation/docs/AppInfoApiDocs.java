package app.external.version.presentation.docs;

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

/** 서버 정보 엔드포인트의 문서 설명. */
public final class AppInfoApiDocs {

  private AppInfoApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "서버 정보", description = "현재 배포된 서버의 버전과 환경 정보를 확인한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "서버 배포 정보를 조회한다",
      description =
          """
          현재 실행 중인 서버의 이름, 환경, 배포된 커밋과 빌드 시각을 반환합니다.

          장애를 확인하거나 클라이언트가 어느 버전과 통신하는지 파악할 때 씁니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "서버 배포 정보",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = AppInfoResult.class))))
  public @interface GetAppInfo {}

  /** 실제 응답은 문자열 맵이다. 담기는 키를 문서에 드러내기 위해 같은 모양의 타입을 선언한다. */
  @Schema(name = "AppInfoResult", title = "서버 배포 정보", description = "서버 이름과 배포된 코드의 출처")
  private record AppInfoResult(
      @Schema(description = "서버 이름", example = "product-api") String serverName,
      @Schema(description = "실행 환경", example = "prod") String environment,
      @Schema(description = "배포된 브랜치", example = "main") String gitBranch,
      @Schema(description = "커밋 해시 앞 7자리", example = "a1b2c3d") String gitCommitHash,
      @Schema(description = "커밋 해시 전체") String gitCommitFullHash,
      @Schema(description = "빌드 시각", example = "2026-07-26 18:00:00") String gitBuildTime) {}
}
