package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserQueryRepository extends UserQueryRepository, JpaRepository<User, Long> {
}
