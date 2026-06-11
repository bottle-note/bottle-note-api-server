package app.bottlenote.common.fixture;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.history.constant.EventCategory;
import app.bottlenote.history.constant.EventType;
import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.user.domain.User;
import java.util.List;

/**
 * 히스토리 테스트용 복합 데이터 record
 *
 * <p>기존 init-user-history.sql 대체용
 */
public record HistoryTestData(
    List<User> users, List<Alcohol> alcohols, List<UserHistory> histories) {

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
   * 특정 사용자의 히스토리 목록 반환
   *
   * @param userId 사용자 ID
   * @return 해당 사용자의 히스토리 목록
   */
  public List<UserHistory> getHistoriesByUser(Long userId) {
    return histories.stream().filter(h -> h.getUserId().equals(userId)).toList();
  }

  /**
   * 특정 이벤트 카테고리의 히스토리 목록 반환
   *
   * @param category 이벤트 카테고리
   * @return 해당 카테고리의 히스토리 목록
   */
  public List<UserHistory> getHistoriesByCategory(EventCategory category) {
    return histories.stream().filter(h -> h.getEventCategory() == category).toList();
  }

  /**
   * 특정 이벤트 타입의 히스토리 목록 반환
   *
   * @param eventType 이벤트 타입
   * @return 해당 타입의 히스토리 목록
   */
  public List<UserHistory> getHistoriesByEventType(EventType eventType) {
    return histories.stream().filter(h -> h.getEventType() == eventType).toList();
  }
}
