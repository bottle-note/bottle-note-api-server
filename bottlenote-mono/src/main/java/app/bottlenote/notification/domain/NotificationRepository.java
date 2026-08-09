package app.bottlenote.notification.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.notification.dto.dsl.NotificationListCriteria;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 알림(Notification) 저장·조회 포트. Spring/JPA 타입을 노출하지 않는다. */
@DomainRepository
public interface NotificationRepository {

  Notification save(Notification notification);

  Optional<Notification> findById(Long id);

  Optional<Notification> findByIdAndUserId(Long id, Long userId);

  /** 사용자 알림을 id 내림차순으로 조회한다. limit은 pageSize+1(hasNext 판별)을 포함한다. */
  List<Notification> findPageByUserId(NotificationListCriteria criteria);

  /**
   * cursor를 제외한 목록 조회 조건의 전체 알림 수를 반환한다.
   *
   * <p>목록과 동일한 사용자·필터 조건을 적용한다.
   */
  long countByCriteria(NotificationListCriteria criteria);

  long countByUserId(Long userId);

  long countByUserIdAndIsReadFalse(Long userId);

  /**
   * 원본 이벤트와 수신 사용자 조합의 알림 존재 여부를 확인한다.
   *
   * <p>DB UNIQUE 제약과 함께 동일 이벤트 재전달을 멱등 처리한다.
   */
  boolean existsBySourceTypeAndSourceIdAndUserId(
      String sourceType, Long sourceId, Long userId);

  /** 본인 미읽음 알림을 읽음 처리하고 최초 읽음 시각을 기록한다. */
  int markAsReadByIdAndUserId(Long id, Long userId, LocalDateTime readAt);

  /** 미읽음 알림을 모두 읽음 처리하고 갱신 건수를 반환한다. */
  int markAllAsReadByUserId(Long userId, LocalDateTime readAt);
}
