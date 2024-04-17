package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
