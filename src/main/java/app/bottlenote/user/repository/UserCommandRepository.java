package app.bottlenote.user.repository;

import app.bottlenote.user.domain.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCommandRepository extends CrudRepository<User, Long> {

	boolean existsByNickName(String nickname);
	Optional<User> findById(Long id);
}
