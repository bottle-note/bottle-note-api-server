package app.bottlenote.user.repository;

import app.bottlenote.common.annotation.JpaRepositoryImpl;
import app.bottlenote.global.service.cursor.PageResponse;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.FollowRepository;
import app.bottlenote.user.dto.dsl.FollowPageableCriteria;
import app.bottlenote.user.dto.response.FollowerSearchResponse;
import app.bottlenote.user.dto.response.FollowingSearchResponse;
import app.bottlenote.user.facade.payload.FriendItem;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;

@JpaRepositoryImpl
@RequiredArgsConstructor
public class JpaFollowRepositoryImpl implements FollowRepository {

  private final SpringDataJpaFollowRepository springDataJpaFollowRepository;
  private final CustomFollowRepository customFollowRepository;

  @Override
  public Follow save(Follow follow) {
    return springDataJpaFollowRepository.save(follow);
  }

  @Override
  public Optional<Follow> findByUserIdAndFollowUserId(Long userId, Long followUserId) {
    return springDataJpaFollowRepository.findByUserIdAndFollowUserId(userId, followUserId);
  }

  @Override
  public List<FriendItem> getTastingFriendsInfoList(
      Long alcoholId, Long userId, PageRequest pageRequest) {
    return springDataJpaFollowRepository.getTastingFriendsInfoList(alcoholId, userId, pageRequest);
  }

  @Override
  public PageResponse<FollowingSearchResponse> getFollowingList(
      Long userId, FollowPageableCriteria criteria) {
    return customFollowRepository.getFollowingList(userId, criteria);
  }

  @Override
  public PageResponse<FollowerSearchResponse> getFollowerList(
      Long userId, FollowPageableCriteria criteria) {
    return customFollowRepository.getFollowerList(userId, criteria);
  }
}
