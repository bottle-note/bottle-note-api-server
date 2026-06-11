package app.bottlenote.common.fixture;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.history.constant.EventType;
import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.fixture.HistoryTestFactory;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.domain.Picks;
import app.bottlenote.picks.fixture.PicksTestFactory;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 복합 테스트 데이터 생성 헬퍼
 *
 * <p>기존 SQL 파일(init-user-mypage-query.sql, init-user-mybottle-query.sql, init-user-history.sql)을
 * 대체하는 TestFactory 기반 데이터 생성 헬퍼
 */
@Component
@RequiredArgsConstructor
public class TestDataSetupHelper {

  private final UserTestFactory userTestFactory;
  private final AlcoholTestFactory alcoholTestFactory;
  private final ReviewTestFactory reviewTestFactory;
  private final RatingTestFactory ratingTestFactory;
  private final PicksTestFactory picksTestFactory;
  private final HistoryTestFactory historyTestFactory;

  /**
   * 마이페이지 테스트 데이터 생성
   *
   * <p>기존 init-user-mypage-query.sql 대체
   *
   * <ul>
   *   <li>사용자 8명
   *   <li>알코올 5개
   *   <li>리뷰 5개 (user[2], user[3], user[0], user[1], user[4] 각각 1개)
   *   <li>팔로우 관계 3개
   *   <li>별점 3개
   * </ul>
   */
  @Transactional
  public MyPageTestData setupMyPageTestData() {
    // 사용자 8명 생성
    List<User> users = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      users.add(userTestFactory.persistUser());
    }

    // 알코올 5개 생성
    List<Alcohol> alcohols = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      alcohols.add(alcoholTestFactory.persistAlcohol());
    }

    // 리뷰 5개 생성 (기존 SQL: user 3,4,1,2,5 순서 -> 0-indexed: 2,3,0,1,4)
    List<Review> reviews = new ArrayList<>();
    reviews.add(reviewTestFactory.persistReview(users.get(2), alcohols.get(0)));
    reviews.add(reviewTestFactory.persistReview(users.get(3), alcohols.get(0)));
    reviews.add(reviewTestFactory.persistReview(users.get(0), alcohols.get(0)));
    reviews.add(reviewTestFactory.persistReview(users.get(1), alcohols.get(1)));
    reviews.add(reviewTestFactory.persistReview(users.get(4), alcohols.get(1)));

    // 팔로우 관계 3개 생성 (기존 SQL: 1->2, 2->3, 3->1)
    List<Follow> follows = new ArrayList<>();
    follows.add(userTestFactory.persistFollow(users.get(0), users.get(1)));
    follows.add(userTestFactory.persistFollow(users.get(1), users.get(2)));
    follows.add(userTestFactory.persistFollow(users.get(2), users.get(0)));

    // 별점 3개 생성 (기존 SQL: user1-alcohol1, user2-alcohol1, user3-alcohol2)
    List<Rating> ratings = new ArrayList<>();
    ratings.add(ratingTestFactory.persistRating(users.get(0), alcohols.get(0), 5));
    ratings.add(ratingTestFactory.persistRating(users.get(1), alcohols.get(0), 4));
    ratings.add(ratingTestFactory.persistRating(users.get(2), alcohols.get(1), 5));

    return new MyPageTestData(users, alcohols, reviews, follows, ratings);
  }

  /**
   * 마이보틀 테스트 데이터 생성
   *
   * <p>기존 init-user-mybottle-query.sql 대체
   *
   * <ul>
   *   <li>사용자 8명
   *   <li>알코올 8개
   *   <li>팔로우 관계 4개
   *   <li>찜 8개 (PICK 4개, UNPICK 4개)
   *   <li>별점 11개
   * </ul>
   */
  @Transactional
  public MyBottleTestData setupMyBottleTestData() {
    // 사용자 8명 생성
    List<User> users = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      users.add(userTestFactory.persistUser());
    }

    // 알코올 8개 생성
    List<Alcohol> alcohols = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      alcohols.add(alcoholTestFactory.persistAlcohol());
    }

    // 팔로우 관계 4개 생성 (기존 SQL: 8->1, 8->2, 8->3, 3->1 / 0-indexed: 7->0, 7->1, 7->2, 2->0)
    List<Follow> follows = new ArrayList<>();
    follows.add(userTestFactory.persistFollow(users.get(7), users.get(0)));
    follows.add(userTestFactory.persistFollow(users.get(7), users.get(1)));
    follows.add(userTestFactory.persistFollow(users.get(7), users.get(2)));
    follows.add(userTestFactory.persistFollow(users.get(2), users.get(0)));

    // 찜 8개 생성 (기존 SQL: PICK 4개, UNPICK 4개)
    List<Picks> picks = new ArrayList<>();
    picks.add(
        picksTestFactory.persistPicks(
            alcohols.get(1).getId(), users.get(2).getId(), PicksStatus.PICK));
    picks.add(
        picksTestFactory.persistPicks(
            alcohols.get(2).getId(), users.get(3).getId(), PicksStatus.UNPICK));
    picks.add(
        picksTestFactory.persistPicks(
            alcohols.get(0).getId(), users.get(7).getId(), PicksStatus.PICK));
    picks.add(
        picksTestFactory.persistPicks(
            alcohols.get(3).getId(), users.get(3).getId(), PicksStatus.UNPICK));
    picks.add(
        picksTestFactory.persistPicks(
            alcohols.get(4).getId(), users.get(0).getId(), PicksStatus.UNPICK));
    picks.add(
        picksTestFactory.persistPicks(
            alcohols.get(5).getId(), users.get(0).getId(), PicksStatus.UNPICK));
    picks.add(
        picksTestFactory.persistPicks(
            alcohols.get(6).getId(), users.get(0).getId(), PicksStatus.PICK));
    picks.add(
        picksTestFactory.persistPicks(
            alcohols.get(7).getId(), users.get(0).getId(), PicksStatus.PICK));

    // 별점 11개 생성
    List<Rating> ratings = new ArrayList<>();
    ratings.add(ratingTestFactory.persistRating(users.get(2), alcohols.get(0), 4)); // 3.5 -> 4
    ratings.add(ratingTestFactory.persistRating(users.get(5), alcohols.get(0), 4)); // 3.5 -> 4
    ratings.add(ratingTestFactory.persistRating(users.get(2), alcohols.get(1), 4)); // 3.5 -> 4
    ratings.add(ratingTestFactory.persistRating(users.get(7), alcohols.get(1), 4)); // 3.5 -> 4
    ratings.add(ratingTestFactory.persistRating(users.get(5), alcohols.get(1), 4));
    ratings.add(ratingTestFactory.persistRating(users.get(0), alcohols.get(2), 5)); // 4.5 -> 5
    ratings.add(ratingTestFactory.persistRating(users.get(0), alcohols.get(3), 5)); // 4.5 -> 5
    ratings.add(ratingTestFactory.persistRating(users.get(0), alcohols.get(4), 4));
    ratings.add(ratingTestFactory.persistRating(users.get(3), alcohols.get(5), 5));
    ratings.add(ratingTestFactory.persistRating(users.get(0), alcohols.get(6), 5)); // 4.5 -> 5
    ratings.add(ratingTestFactory.persistRating(users.get(3), alcohols.get(0), 1)); // 0.5 -> 1

    return new MyBottleTestData(users, alcohols, follows, picks, ratings);
  }

  /**
   * 히스토리 테스트 데이터 생성
   *
   * <p>기존 init-user-history.sql 대체
   *
   * <ul>
   *   <li>사용자 8명
   *   <li>알코올 5개
   *   <li>사용자 히스토리 5개 (RATING 1개, REVIEW 3개, PICK 1개)
   * </ul>
   */
  @Transactional
  public HistoryTestData setupHistoryTestData() {
    // 사용자 8명 생성
    List<User> users = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      users.add(userTestFactory.persistUser());
    }

    // 알코올 5개 생성
    List<Alcohol> alcohols = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      alcohols.add(alcoholTestFactory.persistAlcohol());
    }

    // 히스토리 5개 생성 (user[0] 기준)
    List<UserHistory> histories = new ArrayList<>();
    histories.add(
        historyTestFactory.persistUserHistory(
            users.get(0).getId(), EventType.START_RATING, alcohols.get(0).getId()));
    histories.add(
        historyTestFactory.persistUserHistory(
            users.get(0).getId(), EventType.REVIEW_CREATE, alcohols.get(0).getId(), "blah blah"));
    histories.add(
        historyTestFactory.persistUserHistory(
            users.get(0).getId(), EventType.REVIEW_CREATE, alcohols.get(1).getId(), "리뷰입니다."));
    histories.add(
        historyTestFactory.persistUserHistory(
            users.get(0).getId(), EventType.REVIEW_CREATE, alcohols.get(2).getId(), "리뷰 등록"));
    histories.add(
        historyTestFactory.persistUserHistory(
            users.get(0).getId(), EventType.UNPICK, alcohols.get(0).getId()));

    return new HistoryTestData(users, alcohols, histories);
  }

  /**
   * 기본 사용자와 알코올만 생성 (단순 테스트용)
   *
   * <p>기존 init-user.sql + init-alcohol.sql 대체 (간소화 버전)
   *
   * @param userCount 생성할 사용자 수
   * @param alcoholCount 생성할 알코올 수
   * @return 사용자 목록과 알코올 목록
   */
  @Transactional
  public SimpleTestData setupSimpleTestData(int userCount, int alcoholCount) {
    List<User> users = new ArrayList<>();
    for (int i = 0; i < userCount; i++) {
      users.add(userTestFactory.persistUser());
    }

    List<Alcohol> alcohols = new ArrayList<>();
    for (int i = 0; i < alcoholCount; i++) {
      alcohols.add(alcoholTestFactory.persistAlcohol());
    }

    return new SimpleTestData(users, alcohols);
  }

  /** 기본 테스트 데이터 record */
  public record SimpleTestData(List<User> users, List<Alcohol> alcohols) {
    public User getUser(int index) {
      return users.get(index);
    }

    public Alcohol getAlcohol(int index) {
      return alcohols.get(index);
    }
  }
}
