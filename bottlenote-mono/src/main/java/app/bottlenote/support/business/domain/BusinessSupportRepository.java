package app.bottlenote.support.business.domain;

import app.bottlenote.support.business.domain.BusinessSupport;
import java.util.List;
import java.util.Optional;

public interface BusinessSupportRepository {
  BusinessSupport save(BusinessSupport businessSupport);

  Optional<BusinessSupport> findById(Long id);

  List<BusinessSupport> findAll();

  Optional<BusinessSupport> findTopByUserIdAndContentOrderByIdDesc(Long userId, String content);

  Optional<BusinessSupport> findByIdAndUserId(Long id, Long userId);

  List<BusinessSupport> findAllByUserId(Long userId);
}
