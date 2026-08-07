package app.external.notification.presentation.docs;

import app.external.notification.data.response.NotificationListResponse;
import app.external.notification.data.response.NotificationMarkAllReadResponse;
import app.external.notification.data.response.NotificationMarkReadResponse;
import app.external.notification.data.response.NotificationUnreadCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 알림함 엔드포인트의 OpenAPI 문서 설명. */
public final class NotificationApiDocs {

  private NotificationApiDocs() {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @Tag(name = "알림함", description = "인증 사용자의 알림 목록을 조회하고 읽음 처리한다")
  public @interface ApiTag {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "내 알림 목록을 조회한다",
      description =
          """
          인증 사용자 본인의 알림을 최신순으로 반환합니다. 타 사용자 알림은 포함되지 않습니다.

          **오류 코드**

          | 코드 | 상태 코드 | 발생 조건 | 설명 |
          | --- | --- | --- | --- |
          | `REQUIRED_USER_ID` | 400 | 액세스 토큰에서 사용자 식별자를 얻지 못했을 때 | 유저 아이디가 필요합니다. |
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "알림 목록",
              content =
                  @Content(schema = @Schema(implementation = NotificationListResponse.class))))
  public @interface GetNotifications {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "미읽음 알림 개수를 조회한다",
      description = "인증 사용자 본인의 미읽음 알림 개수를 반환합니다. 뱃지 표시에 사용합니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "미읽음 개수",
              content =
                  @Content(
                      schema = @Schema(implementation = NotificationUnreadCountResponse.class))))
  public @interface GetUnreadCount {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "알림 하나를 읽음 처리한다",
      description =
          """
          지정한 알림을 읽음 처리합니다. 본인 소유 알림만 대상이며, 없거나 타 사용자 알림이면 404입니다.

          **오류 코드**

          | 코드 | 상태 코드 | 발생 조건 | 설명 |
          | --- | --- | --- | --- |
          | `NOTIFICATION_NOT_FOUND` | 404 | 알림이 없거나 본인 소유가 아닐 때 | 알림을 찾을 수 없습니다. |
          """,
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "읽음 처리 결과",
              content =
                  @Content(schema = @Schema(implementation = NotificationMarkReadResponse.class))))
  public @interface MarkAsRead {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @Operation(
      summary = "알림을 모두 읽음 처리한다",
      description = "인증 사용자 본인의 미읽음 알림을 모두 읽음 처리하고 갱신 건수를 반환합니다.",
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "전체 읽음 처리 결과",
              content =
                  @Content(
                      schema = @Schema(implementation = NotificationMarkAllReadResponse.class))))
  public @interface MarkAllAsRead {}
}
