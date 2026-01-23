package app.bottlenote.common.fixture;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.review.domain.Review;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.User;
import java.util.List;

/**
 * 마이페이지 테스트용 복합 데이터 record
 *
 * <p>기존 init-user-mypage-query.sql 대체용
 */
public record MyPageTestData(
    List<User> users,
    List<Alcohol> alcohols,
    List<Review> reviews,
    List<Follow> follows,
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
   * 특정 사용자의 리뷰 목록 반환
   *
   * @param userId 사용자 ID
   * @return 해당 사용자의 리뷰 목록
   */
  public List<Review> getReviewsByUser(Long userId) {
    return reviews.stream().filter(r -> r.getUserId().equals(userId)).toList();
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

  /**
   * 특정 사용자의 팔로워 목록 반환
   *
   * @param userId 사용자 ID
   * @return 해당 사용자를 팔로우하는 Follow 목록
   */
  public List<Follow> getFollowersByUser(Long userId) {
    return follows.stream().filter(f -> f.getTargetUserId().equals(userId)).toList();
  }
}
