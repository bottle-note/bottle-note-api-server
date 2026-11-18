package app.bottlenote.support.business.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.support.business.domain.BusinessSupport;
import app.bottlenote.support.business.domain.BusinessSupportRepository;
import org.springframework.data.jpa.repository.JpaRepository;

@JpaRepositoryImpl
public interface JpaBusinessSupportRepository
    extends JpaRepository<BusinessSupport, Long>, BusinessSupportRepository {
  // All methods are inherited from JpaRepository and BusinessSupportRepository
}
