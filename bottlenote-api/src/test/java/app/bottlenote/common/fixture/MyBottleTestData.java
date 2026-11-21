package app.bottlenote.common.fixture;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.picks.domain.Picks;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.User;
import java.util.List;

/**
 * 마이보틀 테스트용 복합 데이터 record
 *
 * <p>기존 init-user-mybottle-query.sql 대체용
 */
public record MyBottleTestData(
    List<User> users,
    List<Alcohol> alcohols,
    List<Follow> follows,
    List<Picks> picks,
    List<Rating> ratings) {

  /**
   * 특정 인덱스의 사용자 반환
   *
   * @param index 0-based index
   * @return User 객체
   */
  public User getUser(int index) {
    return users.get(index);
  }

  /**
   * 특정 인덱스의 알코올 반환
   *
   * @param index 0-based index
   * @return Alcohol 객체
   */
  public Alcohol getAlcohol(int index) {
    return alcohols.get(index);
  }

  /**
   * 특정 사용자의 찜 목록 반환
   *
   * @param userId 사용자 ID
   * @return 해당 사용자의 찜 목록
   */
  public List<Picks> getPicksByUser(Long userId) {
    return picks.stream().filter(p -> p.getUserId().equals(userId)).toList();
  }

  /**
   * 특정 사용자의 별점 목록 반환
   *
   * @param userId 사용자 ID
   * @return 해당 사용자의 별점 목록
   */
  public List<Rating> getRatingsByUser(Long userId) {
    return ratings.stream().filter(r -> r.getId().getUserId().equals(userId)).toList();
  }

  /**
   * 특정 사용자가 팔로우하는 목록 반환
   *
   * @param userId 사용자 ID
   * @return 해당 사용자가 팔로우하는 Follow 목록
   */
  public List<Follow> getFollowingsByUser(Long userId) {
    return follows.stream().filter(f -> f.getUserId().equals(userId)).toList();
  }
}
