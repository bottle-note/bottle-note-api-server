package app.bottlenote.user.domain;

import java.util.List;
import java.util.Optional;

/** 유저 정보에 관한 질의에 대한 애그리거트를 정의합니다. */
public interface UserRepository {

  User save(User user);

  Optional<User> findById(Long usrId);

  Optional<User> findByIdForUpdate(Long userId);

  List<User> findAll();

  boolean existsByUserId(Long userId);

  boolean existsByNickName(String nickname);
}
