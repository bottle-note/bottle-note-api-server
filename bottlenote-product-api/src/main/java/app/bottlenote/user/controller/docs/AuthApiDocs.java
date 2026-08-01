package app.bottlenote.user.controller.docs;

import app.bottlenote.user.dto.response.NonceResponse;
import app.bottlenote.user.dto.response.OauthResponse;
import app.bottlenote.user.dto.response.SignupCompleteResponse;
import app.bottlenote.user.dto.response.SignupPendingResponse;
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

          기존 회원은 액세스 토큰과 리프레시 쿠키를 받습니다.
          신규 회원은 일반 토큰 없이 SIGNUP_PENDING 상태와 가입 완료 전용 토큰을 받습니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "기존 회원 토큰 또는 신규 회원 가입 대기 토큰",
              content =
                  @Content(
                      schema =
                          @Schema(oneOf = {OauthResponse.class, SignupPendingResponse.class}))))
  public @interface ExecuteAppleLogin {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "카카오 계정으로 로그인한다",
      description =
          """
          카카오에서 받은 액세스 토큰으로 로그인합니다.

          서버는 그 토큰이 우리 앱에서 발급된 것인지 확인한 뒤 처리합니다.
          기존 회원은 액세스 토큰과 리프레시 쿠키를 받습니다.
          신규 회원은 일반 토큰 없이 SIGNUP_PENDING 상태와 가입 완료 전용 토큰을 받습니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "기존 회원 토큰 또는 신규 회원 가입 대기 토큰",
              content =
                  @Content(
                      schema =
                          @Schema(oneOf = {OauthResponse.class, SignupPendingResponse.class}))))
  public @interface ExecuteKakaoLogin {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "가입 동의를 저장하고 가입을 완료한다",
      description = "가입 완료 전용 토큰과 필수 동의 유형·문서 버전 형식을 검증해 사용자를 활성화합니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "가입 완료",
              content = @Content(schema = @Schema(implementation = SignupCompleteResponse.class))))
  public @interface CompleteSignup {}

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
