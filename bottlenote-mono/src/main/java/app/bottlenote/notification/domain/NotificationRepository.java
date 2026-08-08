package app.bottlenote.notification.domain;

import app.bottlenote.common.annotation.DomainRepository;
import app.bottlenote.notification.dto.dsl.NotificationListCriteria;
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

  long countByUserId(Long userId);

  long countByUserIdAndIsReadFalse(Long userId);

  /** 미읽음 알림을 모두 읽음 처리하고 갱신 건수를 반환한다. */
  int markAllAsReadByUserId(Long userId);
}
