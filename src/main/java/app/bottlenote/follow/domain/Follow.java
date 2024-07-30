package app.bottlenote.follow.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.follow.domain.constant.FollowStatus;
import app.bottlenote.user.domain.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@NoArgsConstructor(access = PROTECTED)
public class Follow extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("로그인 유저")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Comment("팔로우대상 유저")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "follow_user_id")
	private User followUser;

	@Comment("팔로우 상태")
	@Enumerated(EnumType.STRING)
	private FollowStatus status = FollowStatus.FOLLOWING;

	@Builder
	public Follow(Long id, User user, User followUser, FollowStatus status) {
		this.id = id;
		this.user = user;
		this.followUser = followUser;
		this.status = status;
	}

	public Follow updateStatus(FollowStatus followStatus) {

		this.status = followStatus;
		return this;
	}
}
