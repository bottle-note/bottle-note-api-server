package app.bottlenote.operation;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.constant.AlcoholType;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.fixture.AlcoholTestFactory;
import app.bottlenote.history.constant.EventType;
import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.history.fixture.HistoryTestFactory;
import app.bottlenote.like.constant.LikeStatus;
import app.bottlenote.like.domain.Likes;
import app.bottlenote.like.fixture.LikesTestFactory;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.domain.Picks;
import app.bottlenote.picks.fixture.PicksTestFactory;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.rating.fixture.RatingTestFactory;
import app.bottlenote.review.constant.SizeType;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewImage;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.review.fixture.ReviewTestFactory;
import app.bottlenote.support.business.domain.BusinessSupport;
import app.bottlenote.support.business.fixture.BusinessSupportTestFactory;
import app.bottlenote.user.domain.Follow;
import app.bottlenote.user.domain.User;
import app.bottlenote.user.fixture.UserTestFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 모든 TestEntityFactory의 CRUD 기능을 검증하는 통합 테스트
 *
 * <p>각 팩토리의 모든 메서드가 실제 DB(TestContainers) 환경에서 정상 작동하는지 검증
 */
@Tag("integration")
@DisplayName("[integration] TestEntityFactories 검증")
class TestEntityFactoriesIntegrationTest extends IntegrationTestSupport {

  @Autowired UserTestFactory userTestFactory;
  @Autowired AlcoholTestFactory alcoholTestFactory;
  @Autowired RatingTestFactory ratingTestFactory;
  @Autowired BusinessSupportTestFactory businessSupportTestFactory;
  @Autowired ReviewTestFactory reviewTestFactory;
  @Autowired LikesTestFactory likesTestFactory;
  @Autowired PicksTestFactory picksTestFactory;
  @Autowired HistoryTestFactory historyTestFactory;

  @PersistenceContext EntityManager em;

  @Nested
  @DisplayName("UserTestFactory")
  class UserTestFactoryTest {

    @Test
    @DisplayName("기본 User 생성")
    void persistUser() {
      User user = userTestFactory.persistUser();

      assertThat(user.getId()).isNotNull();
      em.clear();
      User found = em.find(User.class, user.getId());
      assertThat(found).isNotNull();
    }

    @Test
    @DisplayName("이메일과 닉네임 지정 User 생성")
    void persistUserWithEmailAndNickName() {
      User user = userTestFactory.persistUser("test", "테스터");

      assertThat(user.getId()).isNotNull();
      assertThat(user.getEmail()).contains("test");
      assertThat(user.getNickName()).contains("테스터");
    }

    @Test
    @DisplayName("빌더 기반 User 생성")
    void persistUserWithBuilder() {
      User user =
          userTestFactory.persistUser(
              User.builder().email("builder@example.com").nickName("빌더테스터"));

      assertThat(user.getId()).isNotNull();
      assertThat(user.getEmail()).isEqualTo("builder@example.com");
      assertThat(user.getNickName()).isEqualTo("빌더테스터");
    }

    @Test
    @DisplayName("사용자 간 팔로우 관계 생성")
    void persistFollow() {
      User follower = userTestFactory.persistUser();
      User following = userTestFactory.persistUser();

      Follow follow = userTestFactory.persistFollow(follower, following);

      assertThat(follow.getId()).isNotNull();
      assertThat(follow.getUserId()).isEqualTo(follower.getId());
      assertThat(follow.getTargetUserId()).isEqualTo(following.getId());
    }

    @Test
    @DisplayName("ID로 팔로우 관계 생성")
    void persistFollowWithIds() {
      User follower = userTestFactory.persistUser();
      User following = userTestFactory.persistUser();

      Follow follow = userTestFactory.persistFollow(follower.getId(), following.getId());

      assertThat(follow.getId()).isNotNull();
    }

    @Test
    @DisplayName("팔로우 관계와 사용자 동시 생성")
    void persistFollowWithUsers() {
      Follow follow = userTestFactory.persistFollowWithUsers();

      assertThat(follow.getId()).isNotNull();
      assertThat(follow.getUserId()).isNotNull();
      assertThat(follow.getTargetUserId()).isNotNull();
    }

    @Test
    @DisplayName("특정 사용자의 팔로워 여럿 생성")
    void persistFollowers() {
      User targetUser = userTestFactory.persistUser();
      List<Follow> followers = userTestFactory.persistFollowers(targetUser, 3);

      assertThat(followers).hasSize(3);
      followers.forEach(
          follow -> {
            assertThat(follow.getId()).isNotNull();
            assertThat(follow.getTargetUserId()).isEqualTo(targetUser.getId());
          });
    }

    @Test
    @DisplayName("특정 사용자가 팔로잉하는 사용자 여럿 생성")
    void persistFollowings() {
      User follower = userTestFactory.persistUser();
      List<Follow> followings = userTestFactory.persistFollowings(follower, 3);

      assertThat(followings).hasSize(3);
      followings.forEach(
          follow -> {
            assertThat(follow.getId()).isNotNull();
            assertThat(follow.getUserId()).isEqualTo(follower.getId());
          });
    }
  }

  @Nested
  @DisplayName("AlcoholTestFactory")
  class AlcoholTestFactoryTest {

    @Test
    @DisplayName("기본 Alcohol 생성")
    void persistAlcohol() {
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      assertThat(alcohol.getId()).isNotNull();
      em.clear();
      Alcohol found = em.find(Alcohol.class, alcohol.getId());
      assertThat(found).isNotNull();
    }

    @Test
    @DisplayName("타입 지정 Alcohol 생성")
    void persistAlcoholWithType() {
      Alcohol alcohol = alcoholTestFactory.persistAlcohol(AlcoholType.WHISKY);

      assertThat(alcohol.getId()).isNotNull();
      assertThat(alcohol.getType()).isEqualTo(AlcoholType.WHISKY);
    }

    @Test
    @DisplayName("이름과 타입 지정 Alcohol 생성")
    void persistAlcoholWithNameAndType() {
      Alcohol alcohol =
          alcoholTestFactory.persistAlcohol("테스트위스키", "Test Whisky", AlcoholType.WHISKY);

      assertThat(alcohol.getId()).isNotNull();
      assertThat(alcohol.getKorName()).contains("테스트위스키");
    }

    @Test
    @DisplayName("정확한 이름으로 Alcohol 생성")
    void persistAlcoholWithName() {
      Alcohol alcohol = alcoholTestFactory.persistAlcoholWithName("맥캘란 12년", "Macallan 12");

      assertThat(alcohol.getId()).isNotNull();
      assertThat(alcohol.getKorName()).isEqualTo("맥캘란 12년");
      assertThat(alcohol.getEngName()).isEqualTo("Macallan 12");
    }

    @Test
    @DisplayName("빌더 기반 Alcohol 생성")
    void persistAlcoholWithBuilder() {
      Alcohol alcohol =
          alcoholTestFactory.persistAlcohol(
              Alcohol.builder()
                  .korName("빌더위스키")
                  .engName("Builder Whisky")
                  .type(AlcoholType.WHISKY));

      assertThat(alcohol.getId()).isNotNull();
      assertThat(alcohol.getKorName()).isEqualTo("빌더위스키");
    }

    @Test
    @DisplayName("여러 Alcohol 생성")
    void persistAlcohols() {
      List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(3);

      assertThat(alcohols).hasSize(3);
      alcohols.forEach(alcohol -> assertThat(alcohol.getId()).isNotNull());
    }

    @Test
    @DisplayName("기본 Region 생성")
    void persistRegion() {
      var region = alcoholTestFactory.persistRegion();

      assertThat(region.getId()).isNotNull();
      assertThat(region.getKorName()).contains("스코틀랜드");
    }

    @Test
    @DisplayName("커스텀 Region 생성")
    void persistRegionWithName() {
      var region = alcoholTestFactory.persistRegion("일본", "Japan");

      assertThat(region.getId()).isNotNull();
      assertThat(region.getKorName()).contains("일본");
    }

    @Test
    @DisplayName("기본 Distillery 생성")
    void persistDistillery() {
      var distillery = alcoholTestFactory.persistDistillery();

      assertThat(distillery.getId()).isNotNull();
      assertThat(distillery.getKorName()).contains("맥캘란");
    }

    @Test
    @DisplayName("커스텀 Distillery 생성")
    void persistDistilleryWithName() {
      var distillery = alcoholTestFactory.persistDistillery("글렌피딕", "Glenfiddich");

      assertThat(distillery.getId()).isNotNull();
      assertThat(distillery.getKorName()).contains("글렌피딕");
    }

    @Test
    @DisplayName("기본 CurationKeyword 생성")
    void persistCurationKeyword() {
      var curation = alcoholTestFactory.persistCurationKeyword();

      assertThat(curation.getId()).isNotNull();
      assertThat(curation.getName()).contains("큐레이션");
    }
  }

  @Nested
  @DisplayName("RatingTestFactory")
  class RatingTestFactoryTest {

    @Test
    @DisplayName("User와 Alcohol로 Rating 생성")
    void persistRatingWithEntities() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      Rating rating = ratingTestFactory.persistRating(user, alcohol, 4);

      assertThat(rating.getId()).isNotNull();
      assertThat(rating.getId().getUserId()).isEqualTo(user.getId());
      assertThat(rating.getId().getAlcoholId()).isEqualTo(alcohol.getId());
      assertThat(rating.getRatingPoint().getRating()).isEqualTo(4);
    }

    @Test
    @DisplayName("ID로 Rating 생성")
    void persistRatingWithIds() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      Rating rating = ratingTestFactory.persistRating(user.getId(), alcohol.getId(), 5);

      assertThat(rating.getId()).isNotNull();
      assertThat(rating.getRatingPoint().getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("빌더 기반 Rating 생성")
    void persistRatingWithBuilder() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      Rating rating =
          ratingTestFactory.persistRating(
              Rating.builder()
                  .id(Rating.RatingId.is(user.getId(), alcohol.getId()))
                  .ratingPoint(RatingPoint.of(3)));

      assertThat(rating.getId()).isNotNull();
      assertThat(rating.getRatingPoint().getRating()).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("BusinessSupportTestFactory")
  class BusinessSupportTestFactoryTest {

    @Test
    @DisplayName("BusinessSupport 생성")
    void persist() {
      User user = userTestFactory.persistUser();
      BusinessSupport support = businessSupportTestFactory.persist(user.getId());

      assertThat(support.getId()).isNotNull();
      em.clear();
      BusinessSupport found = em.find(BusinessSupport.class, support.getId());
      assertThat(found).isNotNull();
    }
  }

  @Nested
  @DisplayName("ReviewTestFactory")
  class ReviewTestFactoryTest {

    @Test
    @DisplayName("User와 Alcohol로 Review 생성")
    void persistReviewWithEntities() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      Review review = reviewTestFactory.persistReview(user, alcohol);

      assertThat(review.getId()).isNotNull();
      em.clear();
      Review found = em.find(Review.class, review.getId());
      assertThat(found).isNotNull();
    }

    @Test
    @DisplayName("ID로 Review 생성")
    void persistReviewWithIds() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      Review review = reviewTestFactory.persistReview(user.getId(), alcohol.getId());

      assertThat(review.getId()).isNotNull();
    }

    @Test
    @DisplayName("상세 정보 지정 Review 생성")
    void persistReviewWithDetails() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      Review review =
          reviewTestFactory.persistReview(
              user, alcohol, "테스트 리뷰 내용", SizeType.BOTTLE, BigDecimal.valueOf(50000));

      assertThat(review.getId()).isNotNull();
      assertThat(review.getContent()).isEqualTo("테스트 리뷰 내용");
      assertThat(review.getSizeType()).isEqualTo(SizeType.BOTTLE);
      assertThat(review.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(50000));
    }

    @Test
    @DisplayName("빌더 기반 Review 생성")
    void persistReviewWithBuilder() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      Review review =
          reviewTestFactory.persistReview(
              Review.builder().userId(user.getId()).alcoholId(alcohol.getId()).content("빌더 리뷰"));

      assertThat(review.getId()).isNotNull();
      assertThat(review.getContent()).isEqualTo("빌더 리뷰");
    }

    @Test
    @DisplayName("Review와 User로 ReviewReply 생성")
    void persistReviewReplyWithEntities() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(user, alcohol);

      ReviewReply reply = reviewTestFactory.persistReviewReply(review, user);

      assertThat(reply.getId()).isNotNull();
    }

    @Test
    @DisplayName("ID로 ReviewReply 생성")
    void persistReviewReplyWithIds() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(user, alcohol);

      ReviewReply reply = reviewTestFactory.persistReviewReply(review.getId(), user.getId());

      assertThat(reply.getId()).isNotNull();
    }

    @Test
    @DisplayName("내용 지정 ReviewReply 생성")
    void persistReviewReplyWithContent() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(user, alcohol);

      ReviewReply reply = reviewTestFactory.persistReviewReply(review, user, "테스트 댓글");

      assertThat(reply.getId()).isNotNull();
      assertThat(reply.getContent()).isEqualTo("테스트 댓글");
    }

    @Test
    @DisplayName("대댓글(nested) ReviewReply 생성")
    void persistNestedReviewReply() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(user, alcohol);
      ReviewReply parentReply = reviewTestFactory.persistReviewReply(review, user);

      ReviewReply nestedReply =
          reviewTestFactory.persistReviewReply(review, user, parentReply, "대댓글");

      assertThat(nestedReply.getId()).isNotNull();
      assertThat(nestedReply.getContent()).isEqualTo("대댓글");
      assertThat(nestedReply.getParentReviewReply()).isNotNull();
      assertThat(nestedReply.getParentReviewReply().getId()).isEqualTo(parentReply.getId());
    }

    @Test
    @DisplayName("URL 지정 ReviewImage 생성")
    void persistReviewImageWithUrl() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(user, alcohol);

      ReviewImage image =
          reviewTestFactory.persistReviewImage(review, "https://example.com/image.jpg");

      assertThat(image.getId()).isNotNull();
      assertThat(image.getReviewImageInfo().getImageUrl())
          .isEqualTo("https://example.com/image.jpg");
    }

    @Test
    @DisplayName("여러 ReviewImage 생성")
    void persistReviewImages() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(user, alcohol);

      List<ReviewImage> images = reviewTestFactory.persistReviewImages(review, 3);

      assertThat(images).hasSize(3);
      images.forEach(image -> assertThat(image.getId()).isNotNull());
    }
  }

  @Nested
  @DisplayName("LikesTestFactory")
  class LikesTestFactoryTest {

    @Test
    @DisplayName("기본 Likes 생성")
    void persistLikesWithIds() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(user, alcohol);

      Likes likes = likesTestFactory.persistLikes(review.getId(), user.getId());

      assertThat(likes.getId()).isNotNull();
      em.clear();
      Likes found = em.find(Likes.class, likes.getId());
      assertThat(found).isNotNull();
    }

    @Test
    @DisplayName("닉네임 지정 Likes 생성")
    void persistLikesWithNickName() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(user, alcohol);

      Likes likes = likesTestFactory.persistLikes(review.getId(), user.getId(), "좋아요테스터");

      assertThat(likes.getId()).isNotNull();
      assertThat(likes.getUserInfo().getUserNickName()).isEqualTo("좋아요테스터");
    }

    @Test
    @DisplayName("상태 지정 Likes 생성")
    void persistLikesWithStatus() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(user, alcohol);

      Likes likes = likesTestFactory.persistLikes(review.getId(), user.getId(), LikeStatus.DISLIKE);

      assertThat(likes.getId()).isNotNull();
      assertThat(likes.getStatus()).isEqualTo(LikeStatus.DISLIKE);
    }

    @Test
    @DisplayName("빌더 기반 Likes 생성")
    void persistLikesWithBuilder() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(user, alcohol);

      Likes likes = likesTestFactory.persistLikes(Likes.builder().reviewId(review.getId()));

      assertThat(likes.getId()).isNotNull();
    }

    @Test
    @DisplayName("여러 Likes 생성 (같은 리뷰에 여러 사용자)")
    void persistMultipleLikes() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(user, alcohol);

      List<Likes> likesList = likesTestFactory.persistMultipleLikes(review.getId(), 5);

      assertThat(likesList).hasSize(5);
      likesList.forEach(likes -> assertThat(likes.getId()).isNotNull());
    }

    @Test
    @DisplayName("여러 Likes 생성 (사용자가 여러 리뷰에)")
    void persistMultipleLikesByUser() {
      User user = userTestFactory.persistUser();

      List<Likes> likesList = likesTestFactory.persistMultipleLikesByUser(user.getId(), 3);

      assertThat(likesList).hasSize(3);
      likesList.forEach(
          likes -> {
            assertThat(likes.getId()).isNotNull();
            assertThat(likes.getUserInfo().getUserId()).isEqualTo(user.getId());
          });
    }
  }

  @Nested
  @DisplayName("PicksTestFactory")
  class PicksTestFactoryTest {

    @Test
    @DisplayName("기본 Picks 생성")
    void persistPicksWithIds() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      Picks picks = picksTestFactory.persistPicks(alcohol.getId(), user.getId());

      assertThat(picks.getId()).isNotNull();
      em.clear();
      Picks found = em.find(Picks.class, picks.getId());
      assertThat(found).isNotNull();
    }

    @Test
    @DisplayName("상태 지정 Picks 생성")
    void persistPicksWithStatus() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      Picks picks =
          picksTestFactory.persistPicks(alcohol.getId(), user.getId(), PicksStatus.UNPICK);

      assertThat(picks.getId()).isNotNull();
      assertThat(picks.getStatus()).isEqualTo(PicksStatus.UNPICK);
    }

    @Test
    @DisplayName("빌더 기반 Picks 생성")
    void persistPicksWithBuilder() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      Picks picks =
          picksTestFactory.persistPicks(
              Picks.builder().alcoholId(alcohol.getId()).userId(user.getId()));

      assertThat(picks.getId()).isNotNull();
    }

    @Test
    @DisplayName("여러 Picks 생성 (사용자가 여러 술)")
    void persistMultiplePicksByUser() {
      User user = userTestFactory.persistUser();

      List<Picks> picksList = picksTestFactory.persistMultiplePicksByUser(user.getId(), 4);

      assertThat(picksList).hasSize(4);
      picksList.forEach(
          picks -> {
            assertThat(picks.getId()).isNotNull();
            assertThat(picks.getUserId()).isEqualTo(user.getId());
          });
    }

    @Test
    @DisplayName("여러 Picks 생성 (여러 사용자가 한 술)")
    void persistMultiplePicksByAlcohol() {
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      List<Picks> picksList = picksTestFactory.persistMultiplePicksByAlcohol(alcohol.getId(), 3);

      assertThat(picksList).hasSize(3);
      picksList.forEach(
          picks -> {
            assertThat(picks.getId()).isNotNull();
            assertThat(picks.getAlcoholId()).isEqualTo(alcohol.getId());
          });
    }
  }

  @Nested
  @DisplayName("HistoryTestFactory")
  class HistoryTestFactoryTest {

    @Test
    @DisplayName("기본 UserHistory 생성")
    void persistUserHistoryWithBasic() {
      User user = userTestFactory.persistUser();

      UserHistory history =
          historyTestFactory.persistUserHistory(user.getId(), EventType.REVIEW_CREATE);

      assertThat(history.getId()).isNotNull();
      assertThat(history.getUserId()).isEqualTo(user.getId());
      assertThat(history.getEventType()).isEqualTo(EventType.REVIEW_CREATE);
      em.clear();
      UserHistory found = em.find(UserHistory.class, history.getId());
      assertThat(found).isNotNull();
    }

    @Test
    @DisplayName("alcoholId 포함 UserHistory 생성")
    void persistUserHistoryWithAlcoholId() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      UserHistory history =
          historyTestFactory.persistUserHistory(
              user.getId(), EventType.REVIEW_CREATE, alcohol.getId());

      assertThat(history.getId()).isNotNull();
      assertThat(history.getAlcoholId()).isEqualTo(alcohol.getId());
    }

    @Test
    @DisplayName("content 포함 UserHistory 생성")
    void persistUserHistoryWithContent() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      UserHistory history =
          historyTestFactory.persistUserHistory(
              user.getId(), EventType.REVIEW_CREATE, alcohol.getId(), "테스트 히스토리 내용");

      assertThat(history.getId()).isNotNull();
      assertThat(history.getContent()).isEqualTo("테스트 히스토리 내용");
    }

    @Test
    @DisplayName("모든 필드 지정 UserHistory 생성")
    void persistUserHistoryWithAllFields() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      UserHistory history =
          historyTestFactory.persistUserHistory(
              user.getId(),
              EventType.REVIEW_CREATE,
              alcohol.getId(),
              "https://example.com/redirect",
              "https://example.com/image.jpg",
              "전체 내용");

      assertThat(history.getId()).isNotNull();
      assertThat(history.getRedirectUrl()).isEqualTo("https://example.com/redirect");
      assertThat(history.getImageUrl()).isEqualTo("https://example.com/image.jpg");
      assertThat(history.getContent()).isEqualTo("전체 내용");
    }

    @Test
    @DisplayName("빌더 기반 UserHistory 생성")
    void persistUserHistoryWithBuilder() {
      User user = userTestFactory.persistUser();

      UserHistory history =
          historyTestFactory.persistUserHistory(
              UserHistory.builder()
                  .userId(user.getId())
                  .eventCategory(EventType.IS_PICK.getEventCategory())
                  .eventType(EventType.IS_PICK)
                  .eventYear("2025")
                  .eventMonth("01"));

      assertThat(history.getId()).isNotNull();
      assertThat(history.getEventType()).isEqualTo(EventType.IS_PICK);
    }

    @Test
    @DisplayName("여러 UserHistory 생성 (다양한 이벤트 타입)")
    void persistMultipleUserHistories() {
      User user = userTestFactory.persistUser();

      List<UserHistory> histories =
          historyTestFactory.persistMultipleUserHistories(user.getId(), 5);

      assertThat(histories).hasSize(5);
      histories.forEach(history -> assertThat(history.getId()).isNotNull());
    }

    @Test
    @DisplayName("여러 UserHistory 생성 (같은 이벤트 타입)")
    void persistMultipleUserHistoriesWithSameType() {
      User user = userTestFactory.persistUser();

      List<UserHistory> histories =
          historyTestFactory.persistMultipleUserHistories(user.getId(), EventType.REVIEW_CREATE, 3);

      assertThat(histories).hasSize(3);
      histories.forEach(
          history -> {
            assertThat(history.getId()).isNotNull();
            assertThat(history.getEventType()).isEqualTo(EventType.REVIEW_CREATE);
          });
    }

    @Test
    @DisplayName("다이나믹 메시지 포함 UserHistory 생성")
    void persistUserHistoryWithDynamicMessage() {
      User user = userTestFactory.persistUser();
      Map<String, String> dynamicMessage = new HashMap<>();
      dynamicMessage.put("key1", "value1");
      dynamicMessage.put("key2", "value2");

      UserHistory history =
          historyTestFactory.persistUserHistoryWithDynamicMessage(
              user.getId(), EventType.RATING_MODIFY, dynamicMessage);

      assertThat(history.getId()).isNotNull();
      assertThat(history.getDynamicMessage()).containsEntry("key1", "value1");
      assertThat(history.getDynamicMessage()).containsEntry("key2", "value2");
    }
  }
}
