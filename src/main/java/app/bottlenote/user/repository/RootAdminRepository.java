package app.bottlenote.user.repository;

import app.bottlenote.user.domain.RootAdmin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RootAdminRepository extends JpaRepository<RootAdmin, Long> {
	boolean existsByUserId(Long userId);
}
