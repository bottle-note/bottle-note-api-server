package app.bottlenote.user.domain;

import app.bottlenote.common.annotation.DomainRepository;
import java.util.Optional;

@DomainRepository
public interface FollowRepository {

  Follow save(Follow follow);

  Optional<Follow> findByUserIdAndFollowUserId(Long userId, Long followUserId);
}
