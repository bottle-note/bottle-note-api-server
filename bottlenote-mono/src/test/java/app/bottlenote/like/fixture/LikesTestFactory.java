package app.bottlenote.like.fixture;

import app.bottlenote.like.constant.LikeStatus;
import app.bottlenote.like.domain.LikeUserInfo;
import app.bottlenote.like.domain.Likes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

/**
 * Likes 엔티티 테스트 팩토리
 *
 * <p>테스트에서 Likes 엔티티를 생성하고 영속화하는 헬퍼 클래스
 *
 * <p>철학: 1. 단일 책임: 엔티티 생성과 영속화만 담당 2. 격리: 모든 persist 메서드는 em.flush()를 호출하여 DB 반영 보장 3. 순수성:
 * Repository를 사용하지 않고 EntityManager만 사용 4. 명시성: 모든 파라미터와 반환값에 @NotNull/@Nullable 명시 5. 응집성: 다른 팩토리에
 * 의존하지 않음
 */
@Component
public class LikesTestFactory {

  @PersistenceContext private EntityManager em;
  private final Random random = new Random();

  /** 기본 Likes 생성 (reviewId, userId 지정) */
  @Transactional
  @NotNull
  public Likes persistLikes(@NotNull Long reviewId, @NotNull Long userId) {
    return persistLikes(reviewId, userId, "테스터" + generateRandomSuffix());
  }

  /** Likes 생성 (reviewId, userId, nickName 지정) */
  @Transactional
  @NotNull
  public Likes persistLikes(
      @NotNull Long reviewId, @NotNull Long userId, @NotNull String nickName) {
    LikeUserInfo userInfo = LikeUserInfo.create(userId, nickName);
    Likes likes = Likes.builder().reviewId(reviewId).userInfo(userInfo).build();

    em.persist(likes);
    em.flush();
    return likes;
  }

  /** Likes 생성 (reviewId, userId, status 지정) */
  @Transactional
  @NotNull
  public Likes persistLikes(
      @NotNull Long reviewId, @NotNull Long userId, @NotNull LikeStatus status) {
    LikeUserInfo userInfo = LikeUserInfo.create(userId, "테스터" + generateRandomSuffix());
    Likes likes = Likes.builder().reviewId(reviewId).userInfo(userInfo).status(status).build();

    em.persist(likes);
    em.flush();
    return likes;
  }

  /** 빌더를 사용한 Likes 생성 */
  @Transactional
  @NotNull
  public Likes persistLikes(@NotNull Likes.LikesBuilder builder) {
    Likes tempLikes = builder.build();
    Likes.LikesBuilder finalBuilder = fillMissingLikesFields(tempLikes, builder);

    Likes likes = finalBuilder.build();
    em.persist(likes);
    em.flush();
    return likes;
  }

  /** 여러 Likes 생성 (같은 reviewId에 대해 여러 사용자가 좋아요) */
  @Transactional
  @NotNull
  public List<Likes> persistMultipleLikes(@NotNull Long reviewId, int count) {
    List<Likes> likesList = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Long userId = 1000L + i;
      likesList.add(persistLikes(reviewId, userId));
    }
    return likesList;
  }

  /** 여러 Likes 생성 (특정 사용자가 여러 리뷰에 좋아요) */
  @Transactional
  @NotNull
  public List<Likes> persistMultipleLikesByUser(@NotNull Long userId, int count) {
    List<Likes> likesList = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Long reviewId = 1000L + i;
      likesList.add(persistLikes(reviewId, userId));
    }
    return likesList;
  }

  private String generateRandomSuffix() {
    return String.valueOf(random.nextInt(10000));
  }

  private Likes.LikesBuilder fillMissingLikesFields(Likes tempLikes, Likes.LikesBuilder builder) {
    if (tempLikes.getReviewId() == null) {
      throw new IllegalArgumentException("Likes 생성을 위해 reviewId가 필요합니다.");
    }
    if (tempLikes.getUserInfo() == null) {
      builder.userInfo(LikeUserInfo.create(1L, "기본테스터" + generateRandomSuffix()));
    }
    return builder;
  }
}
