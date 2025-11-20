package app.bottlenote.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import app.bottlenote.IntegrationTestSupport;
import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.alcohols.domain.constant.AlcoholCategoryGroup;
import app.bottlenote.history.constant.EventType;
import app.bottlenote.history.domain.UserHistory;
import app.bottlenote.like.constant.LikeStatus;
import app.bottlenote.like.domain.Likes;
import app.bottlenote.picks.constant.PicksStatus;
import app.bottlenote.picks.domain.Picks;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.review.domain.Review;
import app.bottlenote.review.domain.ReviewImage;
import app.bottlenote.review.domain.ReviewReply;
import app.bottlenote.support.domain.BusinessSupport;
import app.bottlenote.user.domain.User;
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
    @DisplayName("이메일 지정 User 생성")
    void persistUserWithEmail() {
      User user = userTestFactory.persistUser("test@example.com");

      assertThat(user.getId()).isNotNull();
      assertThat(user.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("닉네임 지정 User 생성")
    void persistUserWithNickName() {
      User user = userTestFactory.persistUser("테스터", "test@example.com");

      assertThat(user.getId()).isNotNull();
      assertThat(user.getNickName()).isEqualTo("테스터");
      assertThat(user.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("모든 정보 지정 User 생성")
    void persistUserWithAllInfo() {
      User user = userTestFactory.persistUser("테스터", "test@example.com", "profile.jpg", "M", 1990);

      assertThat(user.getId()).isNotNull();
      assertThat(user.getNickName()).isEqualTo("테스터");
      assertThat(user.getEmail()).isEqualTo("test@example.com");
      assertThat(user.getImageUrl()).isEqualTo("profile.jpg");
      assertThat(user.getGender()).isEqualTo("M");
      assertThat(user.getAge()).isEqualTo(1990);
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
    @DisplayName("여러 User 생성")
    void persistUsers() {
      List<User> users = userTestFactory.persistUsers(5);

      assertThat(users).hasSize(5);
      users.forEach(user -> assertThat(user.getId()).isNotNull());
    }

    @Test
    @DisplayName("팔로워 User 생성")
    void persistFollowerUser() {
      User user = userTestFactory.persistFollowerUser();

      assertThat(user.getId()).isNotNull();
      assertThat(user.getNickName()).contains("팔로워");
    }

    @Test
    @DisplayName("팔로잉 User 생성")
    void persistFollowingUser() {
      User user = userTestFactory.persistFollowingUser();

      assertThat(user.getId()).isNotNull();
      assertThat(user.getNickName()).contains("팔로잉");
    }

    @Test
    @DisplayName("사용자 ID로 User 조회")
    void getUserById() {
      User user = userTestFactory.persistUser();
      User found = userTestFactory.getUserById(user.getId());

      assertThat(found).isNotNull();
      assertThat(found.getId()).isEqualTo(user.getId());
    }

    @Test
    @DisplayName("기본 사용자 ID 조회")
    void getDefaultUserId() {
      Long userId = userTestFactory.getDefaultUserId();

      assertThat(userId).isNotNull();
      User found = em.find(User.class, userId);
      assertThat(found).isNotNull();
    }

    @Test
    @DisplayName("임의 사용자 ID 조회")
    void getRandomUserId() {
      Long userId = userTestFactory.getRandomUserId();

      assertThat(userId).isNotNull();
      User found = em.find(User.class, userId);
      assertThat(found).isNotNull();
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
    @DisplayName("이름 지정 Alcohol 생성")
    void persistAlcoholWithName() {
      Alcohol alcohol = alcoholTestFactory.persistAlcohol("테스트위스키");

      assertThat(alcohol.getId()).isNotNull();
      assertThat(alcohol.getName()).contains("테스트위스키");
    }

    @Test
    @DisplayName("카테고리 지정 Alcohol 생성")
    void persistAlcoholWithCategory() {
      Alcohol alcohol = alcoholTestFactory.persistAlcohol("위스키", AlcoholCategoryGroup.WHISKY);

      assertThat(alcohol.getId()).isNotNull();
      assertThat(alcohol.getCategory()).isEqualTo(AlcoholCategoryGroup.WHISKY);
    }

    @Test
    @DisplayName("빌더 기반 Alcohol 생성")
    void persistAlcoholWithBuilder() {
      Alcohol alcohol =
          alcoholTestFactory.persistAlcohol(
              Alcohol.builder().korName("빌더위스키").korCategoryGroup(AlcoholCategoryGroup.WHISKY));

      assertThat(alcohol.getId()).isNotNull();
      assertThat(alcohol.getName()).contains("빌더위스키");
    }

    @Test
    @DisplayName("여러 Alcohol 생성")
    void persistAlcohols() {
      List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(3);

      assertThat(alcohols).hasSize(3);
      alcohols.forEach(alcohol -> assertThat(alcohol.getId()).isNotNull());
    }

    @Test
    @DisplayName("카테고리별 여러 Alcohol 생성")
    void persistAlcoholsByCategory() {
      List<Alcohol> alcohols = alcoholTestFactory.persistAlcohols(3, AlcoholCategoryGroup.WHISKY);

      assertThat(alcohols).hasSize(3);
      alcohols.forEach(
          alcohol -> {
            assertThat(alcohol.getId()).isNotNull();
            assertThat(alcohol.getCategory()).isEqualTo(AlcoholCategoryGroup.WHISKY);
          });
    }

    @Test
    @DisplayName("AlcoholQuerySupporter용 Alcohol 생성")
    void persistAlcoholsForSupporter() {
      Alcohol alcohol = alcoholTestFactory.persistAlcoholsForSupporter();

      assertThat(alcohol.getId()).isNotNull();
    }

    @Test
    @DisplayName("별점과 리뷰가 있는 Alcohol 생성")
    void persistAlcoholWithRatingAndReview() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcoholWithRatingAndReview(user);

      assertThat(alcohol.getId()).isNotNull();
    }

    @Test
    @DisplayName("별점이 있는 Alcohol 생성")
    void persistAlcoholWithRating() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcoholWithRating(user);

      assertThat(alcohol.getId()).isNotNull();
    }

    @Test
    @DisplayName("리뷰가 있는 Alcohol 생성")
    void persistAlcoholWithReview() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcoholWithReview(user);

      assertThat(alcohol.getId()).isNotNull();
    }

    @Test
    @DisplayName("알코올 ID로 Alcohol 조회")
    void getAlcoholById() {
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Alcohol found = alcoholTestFactory.getAlcoholById(alcohol.getId());

      assertThat(found).isNotNull();
      assertThat(found.getId()).isEqualTo(alcohol.getId());
    }

    @Test
    @DisplayName("기본 알코올 ID 조회")
    void getDefaultAlcoholId() {
      Long alcoholId = alcoholTestFactory.getDefaultAlcoholId();

      assertThat(alcoholId).isNotNull();
      Alcohol found = em.find(Alcohol.class, alcoholId);
      assertThat(found).isNotNull();
    }

    @Test
    @DisplayName("임의 알코올 ID 조회")
    void getRandomAlcoholId() {
      Long alcoholId = alcoholTestFactory.getRandomAlcoholId();

      assertThat(alcoholId).isNotNull();
      Alcohol found = em.find(Alcohol.class, alcoholId);
      assertThat(found).isNotNull();
    }

    @Test
    @DisplayName("알코올 ID 리스트 조회")
    void getAlcoholIds() {
      alcoholTestFactory.persistAlcohols(5);
      List<Long> ids = alcoholTestFactory.getAlcoholIds();

      assertThat(ids).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("여러 알코올 ID 조회")
    void getAlcoholIdsWithCount() {
      List<Long> ids = alcoholTestFactory.getAlcoholIds(3);

      assertThat(ids).hasSize(3);
      ids.forEach(id -> assertThat(em.find(Alcohol.class, id)).isNotNull());
    }

    @Test
    @DisplayName("기본 Alcohol 조회")
    void getDefaultAlcohol() {
      Alcohol alcohol = alcoholTestFactory.getDefaultAlcohol();

      assertThat(alcohol).isNotNull();
      assertThat(alcohol.getId()).isNotNull();
    }

    @Test
    @DisplayName("임의 Alcohol 조회")
    void getRandomAlcohol() {
      Alcohol alcohol = alcoholTestFactory.getRandomAlcohol();

      assertThat(alcohol).isNotNull();
      assertThat(alcohol.getId()).isNotNull();
    }
  }

  @Nested
  @DisplayName("RatingTestFactory")
  class RatingTestFactoryTest {

    @Test
    @DisplayName("기본 Rating 생성")
    void persistRating() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      Rating rating = ratingTestFactory.persistRating(user, alcohol);

      assertThat(rating.getId()).isNotNull();
      assertThat(rating.getId().getUserId()).isEqualTo(user.getId());
      assertThat(rating.getId().getAlcoholId()).isEqualTo(alcohol.getId());
    }

    @Test
    @DisplayName("ID로 Rating 생성")
    void persistRatingWithIds() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      Rating rating = ratingTestFactory.persistRating(user.getId(), alcohol.getId());

      assertThat(rating.getId()).isNotNull();
    }

    @Test
    @DisplayName("별점 지정 Rating 생성")
    void persistRatingWithRatingPoint() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      Rating rating = ratingTestFactory.persistRating(user, alcohol, 4.5);

      assertThat(rating.getId()).isNotNull();
      assertThat(rating.getRating()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("빌더 기반 Rating 생성")
    void persistRatingWithBuilder() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      Rating rating =
          ratingTestFactory.persistRating(Rating.builder().user(user).alcohol(alcohol).rating(5.0));

      assertThat(rating.getId()).isNotNull();
      assertThat(rating.getRating()).isEqualTo(5.0);
    }
  }

  @Nested
  @DisplayName("BusinessSupportTestFactory")
  class BusinessSupportTestFactoryTest {

    @Test
    @DisplayName("BusinessSupport 생성")
    void persistBusinessSupport() {
      BusinessSupport support = businessSupportTestFactory.persistBusinessSupport();

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
    @DisplayName("기본 Review 생성 (User, Alcohol)")
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
    @DisplayName("기본 Review 생성 (userId, alcoholId)")
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
              user, alcohol, "테스트 리뷰 내용", Review.SizeType.BOTTLE, BigDecimal.valueOf(50000));

      assertThat(review.getId()).isNotNull();
      assertThat(review.getContent()).isEqualTo("테스트 리뷰 내용");
      assertThat(review.getSizeType()).isEqualTo(Review.SizeType.BOTTLE);
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
    @DisplayName("여러 Review 생성")
    void persistReviews() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();

      List<Review> reviews = reviewTestFactory.persistReviews(user, alcohol, 3);

      assertThat(reviews).hasSize(3);
      reviews.forEach(review -> assertThat(review.getId()).isNotNull());
    }

    @Test
    @DisplayName("기본 ReviewReply 생성 (Review, User)")
    void persistReviewReplyWithEntities() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(user, alcohol);

      ReviewReply reply = reviewTestFactory.persistReviewReply(review, user);

      assertThat(reply.getId()).isNotNull();
    }

    @Test
    @DisplayName("기본 ReviewReply 생성 (reviewId, userId)")
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
      assertThat(nestedReply.getParentReplyId()).isEqualTo(parentReply.getId());
    }

    @Test
    @DisplayName("빌더 기반 ReviewReply 생성")
    void persistReviewReplyWithBuilder() {
      User user = userTestFactory.persistUser();
      Alcohol alcohol = alcoholTestFactory.persistAlcohol();
      Review review = reviewTestFactory.persistReview(user, alcohol);

      ReviewReply reply =
          reviewTestFactory.persistReviewReply(
              ReviewReply.builder().reviewId(review.getId()).userId(user.getId()).content("빌더 댓글"));

      assertThat(reply.getId()).isNotNull();
      assertThat(reply.getContent()).isEqualTo("빌더 댓글");
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
    @DisplayName("기본 Likes 생성 (reviewId, userId)")
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
    @DisplayName("기본 Picks 생성 (alcoholId, userId)")
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
    @DisplayName("기본 UserHistory 생성 (userId, eventType)")
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
