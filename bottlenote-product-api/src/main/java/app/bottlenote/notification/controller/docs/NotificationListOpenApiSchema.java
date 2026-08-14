package app.bottlenote.notification.controller.docs;

import app.bottlenote.notification.constant.NotificationActionFallbackType;
import app.bottlenote.notification.constant.NotificationActionType;
import app.bottlenote.notification.constant.NotificationCategory;
import app.bottlenote.notification.constant.NotificationStatus;
import app.bottlenote.notification.constant.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 알림 목록 응답을 문서화하기 위한 OpenAPI 전용 schema다.
 *
 * <p>실제 응답의 읽음 상태와 의미 기반 Action 구조를 API 문서에 고정한다.
 */
@Schema(name = "NotificationListResponse", description = "필터 조건에 맞는 알림 목록")
public record NotificationListOpenApiSchema(
    @Schema(description = "id 내림차순 알림 목록") List<Item> items) {

  /**
   * 알림 목록에 포함되는 단일 항목 schema다.
   *
   * <p>읽음 상태와 전달 상태, 이동 Action을 서로 분리해 문서화한다.
   */
  @Schema(name = "NotificationListItem", description = "읽음 상태와 전달 상태를 분리한 알림 항목")
  public record Item(
      @Schema(description = "알림 식별자", example = "101") Long id,
      @Schema(description = "알림 제목") String title,
      @Schema(description = "알림 내용") String content,
      @Schema(description = "알림 타입") NotificationType type,
      @Schema(description = "알림 카테고리") NotificationCategory category,
      @Schema(description = "PENDING/SENT/FAILED 전달 상태이며 읽음 여부와 무관함") NotificationStatus status,
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

  /**
   * 앱과 웹이 내부 route로 변환할 Action schema다.
   *
   * <p>허용된 타입과 payload 버전, 공통 fallback을 함께 제공한다.
   */
  @Schema(name = "NotificationAction", description = "앱과 웹이 내부 route로 변환하는 의미 기반 Action")
  public record Action(
      @Schema(description = "허용된 Action 타입", example = "OPEN_REVIEW") NotificationActionType type,
      @Schema(description = "Action 타입에 따른 이동 대상 식별자", example = "10") Long targetId,
      @Schema(
              description = "Action 타입별 payload",
              oneOf = {OpenReviewActionPayload.class, OpenHelpActionPayload.class})
          ActionPayload payload,
      @Schema(description = "Action payload 스키마 버전", example = "1") Integer version,
      @Schema(
              description = "이동 대상 삭제 또는 접근 불가 시 사용할 공통 fallback",
              example = "OPEN_NOTIFICATION_CENTER")
          NotificationActionFallbackType fallbackType) {}

  /**
   * Action 타입에 따라 선택되는 payload의 공통 OpenAPI 계약이다.
   *
   * <p>클라이언트는 Action type과 version을 먼저 확인한 뒤 대응 DTO로 해석한다.
   */
  @Schema(
      name = "NotificationActionPayload",
      oneOf = {OpenReviewActionPayload.class, OpenHelpActionPayload.class})
  public sealed interface ActionPayload permits OpenReviewActionPayload, OpenHelpActionPayload {}

  /**
   * 리뷰 상세 화면에서 강조할 댓글 정보를 표현한다.
   *
   * <p>댓글이 삭제된 경우 클라이언트는 강조 없이 리뷰 상세만 표시한다.
   */
  @Schema(name = "OpenReviewActionPayload", description = "리뷰 상세에서 강조할 댓글 정보")
  public record OpenReviewActionPayload(
      @Schema(description = "강조 대상 댓글 식별자, 삭제된 댓글이면 강조를 생략함", example = "20") Long replyId)
      implements ActionPayload {}

  /**
   * 문의 상세 이동에 사용하는 빈 payload schema다.
   *
   * <p>이동 대상은 targetId만 사용하며 추가 필드를 허용하지 않는다.
   */
  @Schema(name = "OpenHelpActionPayload", description = "문의 상세 이동용 빈 payload")
  public record OpenHelpActionPayload() implements ActionPayload {}
}
