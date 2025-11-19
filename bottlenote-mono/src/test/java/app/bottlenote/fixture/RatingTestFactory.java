package app.bottlenote.fixture;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.rating.domain.Rating.RatingId;
import app.bottlenote.rating.domain.RatingPoint;
import app.bottlenote.user.domain.User;
import jakarta.persistence.EntityManager;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RatingTestFactory {

  private final Random random = new Random();

  @Autowired private EntityManager em;

  // ========== Rating 생성 메서드들 ==========

  /** 사용자와 알코올로 Rating 생성 */
  @Transactional
  public Rating persistRating(User user, Alcohol alcohol, int point) {
    Rating rating =
        Rating.builder()
            .id(RatingId.is(user.getId(), alcohol.getId()))
            .ratingPoint(RatingPoint.of(point))
            .build();
    em.persist(rating);
    em.flush();
    return rating;
  }

  /** ID와 평점으로 Rating 생성 */
  @Transactional
  public Rating persistRating(Long userId, Long alcoholId, int point) {
    Rating rating =
        Rating.builder()
            .id(RatingId.is(userId, alcoholId))
            .ratingPoint(RatingPoint.of(point))
            .build();
    em.persist(rating);
    em.flush();
    return rating;
  }

  /** 빌더를 통한 Rating 생성 - 누락 필드 자동 채우기 */
  @Transactional
  public Rating persistRating(Rating.RatingBuilder builder) {
    // 누락 필드 채우기
    Rating.RatingBuilder filledBuilder = fillMissingRatingFields(builder);
    Rating rating = filledBuilder.build();
    em.persist(rating);
    em.flush();
    return rating;
  }

  /** 빌더를 통한 Rating 생성 후 flush (즉시 ID 필요한 경우) */
  @Transactional
  public Rating persistAndFlushRating(Rating.RatingBuilder builder) {
    // 누락 필드 채우기
    Rating.RatingBuilder filledBuilder = fillMissingRatingFields(builder);
    Rating rating = filledBuilder.build();
    em.persist(rating);
    em.flush(); // 즉시 ID 필요한 경우에만 사용
    return rating;
  }

  // ========== 헬퍼 메서드들 ==========

  /** 랜덤 접미사 생성 헬퍼 메서드 */
  private String generateRandomSuffix() {
    return String.valueOf(random.nextInt(10000));
  }

  /** Rating 빌더의 누락 필드 채우기 */
  private Rating.RatingBuilder fillMissingRatingFields(Rating.RatingBuilder builder) {
    // 빌더를 임시로 빌드해서 필드 체크
    Rating tempRating;
    try {
      tempRating = builder.build();
    } catch (Exception e) {
      // 필수 필드 누락 시 기본값으로 채우기 (실제로는 userId, alcoholId가 필요하므로 에러)
      throw new IllegalArgumentException("Rating 생성을 위해서는 userId와 alcoholId가 필요합니다.", e);
    }

    // 개별 필드 체크 및 채우기
    if (tempRating.getId() == null) {
      throw new IllegalArgumentException("Rating ID (userId, alcoholId)가 필요합니다.");
    }
    if (tempRating.getRatingPoint() == null) {
      builder.ratingPoint(RatingPoint.of(3)); // 기본 평점 3점
    }

    return builder;
  }
}
