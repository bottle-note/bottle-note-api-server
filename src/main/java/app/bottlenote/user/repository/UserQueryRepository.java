package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserQueryRepository extends CrudRepository<User, Long> {
}
