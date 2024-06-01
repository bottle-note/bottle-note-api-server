package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import app.bottlenote.user.domain.UserQueryRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUserQueryRepository extends UserQueryRepository, CrudRepository<User, Long> {
}
