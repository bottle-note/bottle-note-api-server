package app.bottlenote.notification.controller.docs;

import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationReadStatus;
import app.bottlenote.notification.constant.NotificationType;
import app.bottlenote.notification.dto.response.NotificationMarkAllReadResponse;
import app.bottlenote.notification.dto.response.NotificationMarkReadResponse;
import app.bottlenote.notification.dto.response.NotificationUnreadCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
          인증 사용자 본인의 알림을 최신순(id desc) keyset 커서 페이징으로 반환합니다. 타 사용자 알림은 포함되지 않습니다.

          - `cursor`: HMAC 커서. 미지정하면 최신부터 조회
          - `size`: 페이지 크기 (기본 10, 최대 100)
          - `types`: 알림 타입 목록. 미지정 또는 빈 목록이면 전체
          - `categories`: 알림 카테고리 목록. 미지정 또는 빈 목록이면 전체
          - `readStatus`: 읽음 필터 (`ALL`, `UNREAD`, `READ`, 기본 `ALL`). `isRead` 기준
          - `createdFrom`: 생성 시각 하한(포함), ISO-8601 OffsetDateTime
          - `createdTo`: 생성 시각 상한(제외), ISO-8601 OffsetDateTime
          - 시각 offset은 동일 instant의 Asia/Seoul 시각으로 정규화하며, from은 to보다 이전이어야 함
          - 다음 페이지는 `meta.pagination.nextCursor`를 그대로 보낸다
          - `isRead/readAt`은 읽음 상태, `status`는 PENDING/SENT/FAILED 전달 상태로 서로 독립
          - `createAt/readAt` 값은 Asia/Seoul `+09:00` offset으로 반환하고 `readAt`은 null일 수 있음

          별도 Action 실행 endpoint는 제공하지 않습니다. 앱과 웹은 응답의 semantic `type`을 각 플랫폼 내부 route로 변환합니다.
          `OPEN_REVIEW` v1은 {replyId}, v2는 빈 payload로 댓글 없는 리뷰를 엽니다. `OPEN_USER` v1은 빈 payload와 targetId로 사용자 프로필을 엽니다.
          클라이언트가 type/version을 지원하지 않으면 알림함으로 fallback합니다.
          `OPEN_REVIEW` 이동 시 리뷰 상세 API가 존재 여부와 접근 권한을 다시 검증합니다. 댓글만 삭제됐으면 리뷰 상세를 열고 댓글 강조를 생략하며,
          리뷰가 삭제됐거나 접근 권한이 없으면 `fallbackType=OPEN_NOTIFICATION_CENTER`를 적용합니다. 서버 응답에는 raw URL 또는 route 문자열을 포함하지 않습니다.

          **오류 코드**

          | 코드 | 상태 코드 | 발생 조건 | 설명 |
          | --- | --- | --- | --- |
          | `REQUIRED_USER_ID` | 400 | 액세스 토큰에서 사용자 식별자를 얻지 못했을 때 | 유저 아이디가 필요합니다. |
          """,
      parameters = {
        @Parameter(
            name = "cursor",
            description = "HMAC 커서. 미지정 시 첫 페이지",
            schema = @Schema(type = "string")),
        @Parameter(
            name = "size",
            description = "페이지 크기",
            example = "10",
            schema = @Schema(type = "integer", format = "int32", minimum = "1", maximum = "100")),
        @Parameter(
            name = "types",
            description = "알림 타입 배열. 미지정 또는 빈 배열이면 전체",
            array = @ArraySchema(schema = @Schema(implementation = NotificationType.class))),
        @Parameter(
            name = "categories",
            description = "알림 카테고리 배열. 미지정 또는 빈 배열이면 전체",
            array = @ArraySchema(schema = @Schema(implementation = NotificationCategory.class))),
        @Parameter(
            name = "readStatus",
            description = "읽음 필터 (ALL, UNREAD, READ)",
            example = "ALL",
            schema = @Schema(implementation = NotificationReadStatus.class)),
        @Parameter(
            name = "createdFrom",
            description = "생성 시각 하한(포함), ISO-8601 OffsetDateTime",
            example = "2026-08-10T00:00:00Z",
            schema = @Schema(type = "string", format = "date-time")),
        @Parameter(
            name = "createdTo",
            description = "생성 시각 상한(제외), ISO-8601 OffsetDateTime",
            example = "2026-08-11T00:00:00Z",
            schema = @Schema(type = "string", format = "date-time"))
      },
      responses =
          @ApiResponse(
              responseCode = "200",
              description = "알림 목록",
              content =
                  @Content(schema = @Schema(implementation = NotificationListOpenApiSchema.class))))
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
          지정한 알림을 멱등하게 읽음 처리합니다. 최초 요청만 `changed=true`이며 반복 요청은 최초 `readAt`을 유지하고 `changed=false`를 반환합니다.
          본인 소유 알림만 대상이며, 없거나 타 사용자 알림이면 404입니다.

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
