package app.external.notification.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.List;
import java.util.Optional;

/** 알림(Notification) 저장·조회 포트. Spring/JPA 타입을 노출하지 않는다. */
@DomainRepository
public interface NotificationRepository {

  Notification save(Notification notification);

  Optional<Notification> findById(Long id);

  Optional<Notification> findByIdAndUserId(Long id, Long userId);

  List<Notification> findAllByUserIdOrderByIdDesc(Long userId);

  long countByUserIdAndIsReadFalse(Long userId);

  /** 미읽음 알림을 모두 읽음 처리하고 갱신 건수를 반환한다. */
  int markAllAsReadByUserId(Long userId);
}
