package app.bottlenote.user.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.user.domain.constant.FollowStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Builder
@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PRIVATE)
@Table(
	name = "follow",
	uniqueConstraints = {
		@UniqueConstraint(name = "unique_user_followUser", columnNames = {"user_id", "follow_user_id"})
	}
)
public class Follow extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("기준 유저 ID")
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Comment("팔로우한 유저 ID")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "follow_user_id")
	private User followUser;

	@Builder.Default
	@Comment("팔로우 상태")
	@Enumerated(EnumType.STRING)
	private FollowStatus status = FollowStatus.FOLLOWING;

	public Follow updateStatus(FollowStatus followStatus) {
		this.status = followStatus;
		return this;
	}
}
