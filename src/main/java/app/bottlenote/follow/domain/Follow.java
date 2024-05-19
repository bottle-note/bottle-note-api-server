package app.bottlenote.follow.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.follow.domain.constant.FollowStatus;
import app.bottlenote.user.domain.User;
import jakarta.persistence.*;
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


	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "follow_user_id")
	private User followUser;


	@Builder
	public Follow(User user, User followUser) {
		this.user = user;
		this.followUser = followUser;
	}

}
