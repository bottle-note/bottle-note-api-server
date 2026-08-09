package app.bottlenote.notification.controller.docs;

import app.bottlenote.notification.constant.NotificationActionFallbackType;
import app.bottlenote.notification.constant.NotificationActionType;
import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationStatus;
import app.bottlenote.notification.constant.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/** 알림 목록 응답의 OpenAPI 전용 schema. */
@Schema(name = "NotificationListResponse", description = "필터 조건에 맞는 알림 목록과 전체 건수")
public record NotificationListOpenApiSchema(
    @Schema(description = "cursor를 제외한 동일 필터 조건의 전체 알림 수", example = "2")
        long totalCount,
    @Schema(description = "id 내림차순 알림 목록") List<Item> items) {

  @Schema(name = "NotificationListItem", description = "읽음 상태와 전달 상태를 분리한 알림 항목")
  public record Item(
      @Schema(description = "알림 식별자", example = "101") Long id,
      @Schema(description = "알림 제목") String title,
      @Schema(description = "알림 내용") String content,
      @Schema(description = "알림 타입") NotificationType type,
      @Schema(description = "알림 카테고리") NotificationCategory category,
      @Schema(description = "PENDING/SENT/FAILED 전달 상태이며 읽음 여부와 무관함")
          NotificationStatus status,
      @Schema(description = "읽음 상태의 SSOT", example = "false") Boolean isRead,
      @Schema(
              description = "알림 생성 시각, Asia/Seoul +09:00 offset",
              example = "2026-08-10T12:00:00+09:00")
          OffsetDateTime createAt,
      @Schema(
              description = "최초 읽음 시각, 미읽음 또는 과거 시각 미상 데이터는 null, 값은 +09:00 offset",
              example = "2026-08-10T12:05:00+09:00",
              nullable = true)
          OffsetDateTime readAt,
      @Schema(description = "의미 기반 이동 Action, 미지원 또는 유효하지 않은 값은 null", nullable = true)
          Action action) {}

  @Schema(name = "NotificationAction", description = "앱과 웹이 내부 route로 변환하는 의미 기반 Action")
  public record Action(
      @Schema(description = "허용된 Action 타입", example = "OPEN_REVIEW")
          NotificationActionType type,
      @Schema(description = "이동 대상 리뷰 식별자", example = "10") Long targetId,
      @Schema(description = "Action 타입별 payload") OpenReviewActionPayload payload,
      @Schema(description = "Action payload 스키마 버전", example = "1") Integer version,
      @Schema(
              description = "리뷰 삭제 또는 접근 불가 시 사용할 공통 fallback",
              example = "OPEN_NOTIFICATION_CENTER")
          NotificationActionFallbackType fallbackType) {}

  @Schema(name = "OpenReviewActionPayload", description = "리뷰 상세에서 강조할 댓글 정보")
  public record OpenReviewActionPayload(
      @Schema(description = "강조 대상 댓글 식별자, 삭제된 댓글이면 강조를 생략함", example = "20")
          Long replyId) {}
}
