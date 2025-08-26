package app.bottlenote.like.domain;

import static lombok.AccessLevel.PROTECTED;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.like.constant.LikeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Comment;

@Builder
@Getter
@Entity(name = "likes")
@Table(name = "likes")
@ToString
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
public class Likes extends BaseEntity {

  @Id
  @Comment("좋아요 식별자")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("리뷰 식별자")
  @JoinColumn(name = "review_id")
  private Long reviewId;

  @Comment("좋아요 사용자 정보")
  @Embedded
  private LikeUserInfo userInfo;

  @Comment("좋아요 상태")
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private LikeStatus status = LikeStatus.LIKE;

  public void updateStatus(LikeStatus status) {
    Objects.requireNonNull(status, "상태값은 null일 수 없습니다.");
    this.status = status;
  }
}
