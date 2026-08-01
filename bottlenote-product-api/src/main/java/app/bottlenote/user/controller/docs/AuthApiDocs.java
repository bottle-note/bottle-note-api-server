package app.bottlenote.user.controller.docs;

import app.bottlenote.user.dto.response.NonceResponse;
import app.bottlenote.user.dto.response.OauthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 로그인과 토큰 관리 엔드포인트의 문서 설명. */
public final class AuthApiDocs {

  private AuthApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "인증", description = "소셜 로그인과 토큰 발급·검증을 처리한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "관리자 권한이 있는지 확인한다",
      description = "현재 로그인한 사용자가 관리자 권한을 가졌는지 확인합니다. 앱에서 관리자 전용 화면을 노출할지 판단할 때 씁니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "관리자 여부",
              content =
                  @Content(
                      schema =
                          @Schema(type = "boolean", example = "false", description = "관리자면 true"))))
  public @interface CheckAdminStatus {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "애플 로그인용 일회성 값을 발급한다",
      description =
          """
          애플 로그인을 시작하기 전에 받아야 하는 일회성 값(nonce)을 발급합니다.

          이 값을 애플 로그인 요청에 포함시켜야 서버가 재사용 공격을 걸러낼 수 있습니다. 한 번 쓰면 무효가 됩니다.
          이 응답은 공통 형식으로 감싸지 않고 값을 그대로 내려줍니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "발급된 일회성 값",
              content = @Content(schema = @Schema(implementation = NonceResponse.class))))
  public @interface GetAppleNonce {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "애플 계정으로 로그인한다",
      description =
          """
          애플에서 받은 인증 토큰과 앞서 발급받은 일회성 값으로 로그인합니다.

          처음 로그인하는 계정이면 회원을 새로 만들고 응답에 첫 로그인 여부를 표시합니다.
          리프레시 토큰은 응답 본문이 아니라 쿠키로 내려갑니다. 이 응답은 공통 형식으로 감싸지 않습니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "액세스 토큰, 첫 로그인 여부, 필수 동의 필요 여부",
              content = @Content(schema = @Schema(implementation = OauthResponse.class))))
  public @interface ExecuteAppleLogin {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "카카오 계정으로 로그인한다",
      description =
          """
          카카오에서 받은 액세스 토큰으로 로그인합니다.

          서버는 그 토큰이 우리 앱에서 발급된 것인지 확인한 뒤 처리합니다.
          처음 로그인하는 계정이면 회원을 새로 만듭니다. 리프레시 토큰은 쿠키로 내려가며 이 응답은 공통 형식으로 감싸지 않습니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "액세스 토큰, 첫 로그인 여부, 필수 동의 필요 여부",
              content = @Content(schema = @Schema(implementation = OauthResponse.class))))
  public @interface ExecuteKakaoLogin {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "에이전트 키로 로그인한다",
      description =
          """
          발급받은 bn_agent_ 접두사의 비밀 API Key로 매핑된 계정에 로그인합니다.

          API Key가 bn_agent_ 접두사와 Base64URL 43자 형식이 아니면 400을 반환합니다. 키가 등록되지 않았거나 비활성화됐거나
          매핑된 계정이 없거나 계정이 비활성 상태인 경우는 모두 동일한 401로 응답해 계정 존재 여부를 노출하지 않습니다.
          리프레시 토큰은 응답 본문이 아니라 쿠키로 내려갑니다. 이 응답은 공통 형식으로 감싸지 않습니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "액세스 토큰과 첫 로그인 여부",
              content = @Content(schema = @Schema(implementation = OauthResponse.class))))
  public @interface ExecuteAgentLogin {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "액세스 토큰을 다시 발급한다",
      description =
          """
          액세스 토큰이 만료됐을 때 리프레시 토큰으로 새 토큰을 받습니다.

          리프레시 토큰은 요청 헤더로 보내며, 새로 발급된 리프레시 토큰은 쿠키로 내려갑니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "새로 발급된 액세스 토큰",
              content = @Content(schema = @Schema(implementation = OauthResponse.class))))
  public @interface ReissueToken {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "토큰이 유효한지 검증한다",
      description = "전달한 토큰이 우리 서버가 발급한 유효한 토큰인지 확인합니다. 만료됐거나 위조된 토큰이면 검증에 실패합니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "검증 결과",
              content = @Content(schema = @Schema(type = "string", description = "검증 결과 메시지"))))
  public @interface VerifyToken {}
}
