package app.bottlenote.history.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Getter
@Entity(name = "alcohols_view_history")
@Table(name = "alcohols_view_history")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlcoholsViewHistory {

	@EmbeddedId
	private AlcoholsViewHistoryId alcoholsViewHistoryId;

	@Comment("조회 시점")
	@Column(name = "view_at", nullable = false)
	private LocalDateTime viewAt;

	public static AlcoholsViewHistory of(Long userId, Long alcoholId,LocalDateTime viewAt) {
		var id = new AlcoholsViewHistoryId(userId, alcoholId);
		return new AlcoholsViewHistory(id, viewAt);
	}

	@Getter
	@Embeddable
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	public static class AlcoholsViewHistoryId {
		@Comment("사용자 ID")
		@Column(name = "user_id", nullable = false)
		private Long userId;

		@Comment("술 ID")
		@Column(name = "alcohol_id", nullable = false)
		private Long alcoholId;
	}

}
