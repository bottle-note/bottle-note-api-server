package app.bottlenote.support.business.repository;

import app.bottlenote.support.business.domain.BusinessSupport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessSupportRepository extends JpaRepository<BusinessSupport, Long> {
	Optional<BusinessSupport> findTopByUserIdAndContentOrderByIdDesc(Long userId, String content);

	Optional<BusinessSupport> findByIdAndUserId(Long id, Long userId);

	List<BusinessSupport> findAllByUserId(Long userId);
}
