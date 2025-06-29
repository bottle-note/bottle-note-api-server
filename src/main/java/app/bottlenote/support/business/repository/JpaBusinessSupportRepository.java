package app.bottlenote.support.business.repository;

import app.bottlenote.support.business.domain.BusinessSupport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaBusinessSupportRepository extends JpaRepository<BusinessSupport, Long>, BusinessSupportRepository {
    // All methods are inherited from JpaRepository and BusinessSupportRepository
}
