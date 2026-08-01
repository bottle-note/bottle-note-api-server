package app.bottlenote.agreement.controller.docs;

import app.bottlenote.agreement.dto.response.AgreementStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class AgreementApiDocs {

  private AgreementApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "사용자 동의", description = "인증 사용자의 약관 동의 상태를 조회하고 의사표시를 기록한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "동의 상태를 조회한다",
      description = "인증 사용자의 유형별 동의 충족 상태와 전체 이용 자격을 조회합니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "동의 상태",
              content = @Content(schema = @Schema(implementation = AgreementStatusResponse.class))))
  public @interface GetStatus {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "동의 의사표시를 제출한다",
      description = "화면에 표시한 문서 원문과 동의 또는 철회 의사표시를 기록하고 갱신된 상태를 반환합니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "갱신된 동의 상태",
              content = @Content(schema = @Schema(implementation = AgreementStatusResponse.class))))
  public @interface Submit {}
}
