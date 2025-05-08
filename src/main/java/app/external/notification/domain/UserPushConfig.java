package app.external.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Comment;

@Getter
@Table(name = "user_push_configs")
@Entity(name = "user_push_config")
public class UserPushConfig {
	@Id
	@Comment("사용자 식별자")
	@Column(name = "user_id", nullable = false, unique = true, updatable = false)
	private Long userId;

	@Comment("이벤트 푸시 알림 여부")
	@Column(name = "event", nullable = false)
	private boolean event;

	@Comment("프로모션 푸시 알림 여부")
	@Column(name = "promotion", nullable = false)
	private boolean promotion;

	@Comment("팔로워 푸시 알림 여부")
	@Column(name = "follower", nullable = false)
	private boolean follower;

	@Comment("댓글 푸시 알림 여부")
	@Column(name = "review", nullable = false)
	private boolean review;

	@Comment("야간 푸시 알림 여부 (22시 ~ 08시)")
	@Column(name = "night", nullable = false)
	private boolean night;
}
