package app.bottlenote.user.controller.docs;

import app.bottlenote.user.dto.response.NicknameChangeResponse;
import app.bottlenote.user.dto.response.ProfileImageChangeResponse;
import app.bottlenote.user.dto.response.WithdrawUserResultResponse;
import app.bottlenote.user.facade.payload.UserProfileItem;
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

/** 회원 정보 관리 엔드포인트의 문서 설명. */
public final class UserBasicApiDocs {

  private UserBasicApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "회원 정보", description = "닉네임과 프로필 이미지를 바꾸고 탈퇴를 처리한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "닉네임을 변경한다",
      description =
          """
          로그인한 사용자의 닉네임을 바꿉니다.

          이미 다른 사용자가 쓰고 있는 닉네임이면 변경할 수 없습니다. 변경 이력은 별도로 기록됩니다.
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "변경된 닉네임",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = NicknameChangeResponse.class))))
  public @interface ChangeNickname {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "프로필 이미지를 변경한다",
      description = "미리 업로드해 받은 이미지 주소로 프로필 이미지를 바꿉니다. 파일 업로드는 별도 엔드포인트에서 처리합니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "변경된 프로필 이미지 주소",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = ProfileImageChangeResponse.class))))
  public @interface ChangeProfileImage {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "회원을 탈퇴한다",
      description = "로그인한 사용자의 계정을 탈퇴 처리합니다. 작성한 리뷰와 별점 등 활동 기록의 처리 방식은 서비스 정책을 따릅니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "탈퇴 처리 결과",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = WithdrawUserResultResponse.class))))
  public @interface WithdrawUser {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "로그인한 사용자의 프로필을 조회한다",
      description = "현재 로그인한 사용자의 식별자, 닉네임, 프로필 이미지를 반환합니다. 앱 상단이나 설정 화면에 표시할 정보입니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "사용자 프로필",
              content =
                  @Content(
                      mediaType = MediaType.APPLICATION_JSON_VALUE,
                      schema = @Schema(implementation = UserProfileItem.class))))
  public @interface GetCurrentUser {}
}
