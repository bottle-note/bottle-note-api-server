package app.bottlenote.history.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@Entity(name = "alcohols_view_history")
@Table(name = "alcohols_view_histories")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlcoholsViewHistory {

  @EmbeddedId private AlcoholsViewHistoryId id;

  @Comment("조회 시점")
  @Column(name = "view_at", nullable = false)
  private LocalDateTime viewAt;

  public static AlcoholsViewHistory of(Long userId, Long alcoholId, LocalDateTime viewAt) {
    var id = new AlcoholsViewHistoryId(userId, alcoholId);
    return new AlcoholsViewHistory(id, viewAt);
  }

  // 조회 시점 갱신
  public void updateViewAt(LocalDateTime viewAt) {
    this.viewAt = viewAt;
  }

  @Getter
  @Embeddable
  @EqualsAndHashCode
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public static class AlcoholsViewHistoryId {
    @Comment("사용자 ID")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Comment("술 ID")
    @Column(name = "alcohol_id", nullable = false)
    private Long alcoholId;

    public static AlcoholsViewHistoryId of(Long userId, Long alcoholId) {
      return new AlcoholsViewHistoryId(userId, alcoholId);
    }
  }
}
