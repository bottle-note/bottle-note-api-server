package app.bottlenote.rating.domain;

import static lombok.AccessLevel.PROTECTED;

import app.bottlenote.core.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("알콜 점수 테이블")
@Entity(name = "rating")
@Table(name = "ratings")
public class Rating extends BaseEntity {

  @EmbeddedId private RatingId id;

  @Embedded
  @Comment("평가점수 : 0, 0.5, 1.0 ... 5.0")
  @Column(name = "rating")
  private RatingPoint ratingPoint = new RatingPoint();

  @Builder
  public Rating(RatingId id, RatingPoint ratingPoint) {
    this.id = id;
    this.ratingPoint = ratingPoint;
  }

  public void registerRatingPoint(RatingPoint ratingPoint) {
    ratingPoint.isValidRating(ratingPoint.getRating());
    this.ratingPoint = ratingPoint;
  }

  @Getter
  @AllArgsConstructor
  @NoArgsConstructor(access = PROTECTED)
  @Embeddable
  public static class RatingId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "alcohol_id")
    private Long alcoholId;

    public static RatingId is(Long userId, Long alcoholId) {
      return new RatingId(userId, alcoholId);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      RatingId ratingId = (RatingId) o;
      return Objects.equals(userId, ratingId.userId)
          && Objects.equals(alcoholId, ratingId.alcoholId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(userId, alcoholId);
    }
  }
}
