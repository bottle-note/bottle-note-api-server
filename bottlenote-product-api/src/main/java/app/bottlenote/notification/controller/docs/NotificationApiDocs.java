package app.bottlenote.notification.controller.docs;

import app.bottlenote.notification.dto.response.NotificationListResponse;
import app.bottlenote.notification.dto.response.NotificationMarkAllReadResponse;
import app.bottlenote.notification.dto.response.NotificationMarkReadResponse;
import app.bottlenote.notification.dto.response.NotificationUnreadCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

          - `cursor`: 직전 페이지 마지막 알림 id. 미지정/0이면 최신부터 조회
          - `pageSize`: 페이지 크기 (기본 10, 최대 100)
          - `types`: 알림 타입 목록. 미지정 또는 빈 목록이면 전체
          - `categories`: 알림 카테고리 목록. 미지정 또는 빈 목록이면 전체
          - `readStatus`: 읽음 필터 (`ALL`, `UNREAD`, `READ`, 기본 `ALL`). `isRead` 기준
          - `createdFrom`: 생성 시각 하한(포함), ISO-8601 OffsetDateTime
          - `createdTo`: 생성 시각 상한(제외), ISO-8601 OffsetDateTime
          - 시각 offset은 동일 instant의 Asia/Seoul 시각으로 정규화하며, from은 to보다 이전이어야 함
          - 응답 `meta.pageable.cursor`는 이번 페이지 마지막 item id(다음 요청용 nextCursor)

          **오류 코드**

          | 코드 | 상태 코드 | 발생 조건 | 설명 |
          | --- | --- | --- | --- |
          | `REQUIRED_USER_ID` | 400 | 액세스 토큰에서 사용자 식별자를 얻지 못했을 때 | 유저 아이디가 필요합니다. |
          """,
      parameters = {
        @Parameter(
            name = "cursor",
            description = "직전 페이지 마지막 알림 id (keyset, 0이면 최초)",
            example = "0"),
        @Parameter(name = "pageSize", description = "페이지 크기", example = "10"),
        @Parameter(name = "types", description = "알림 타입 목록", example = "USER"),
        @Parameter(name = "categories", description = "알림 카테고리 목록", example = "REVIEW"),
        @Parameter(
            name = "readStatus",
            description = "읽음 필터 (ALL, UNREAD, READ)",
            example = "ALL"),
        @Parameter(
            name = "createdFrom",
            description = "생성 시각 하한(포함), ISO-8601 OffsetDateTime",
            example = "2026-08-10T00:00:00Z"),
        @Parameter(
            name = "createdTo",
            description = "생성 시각 상한(제외), ISO-8601 OffsetDateTime",
            example = "2026-08-11T00:00:00Z")
      },
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
