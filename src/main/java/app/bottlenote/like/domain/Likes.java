package app.bottlenote.like.domain;

import app.bottlenote.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

import static lombok.AccessLevel.PROTECTED;

@Builder
@Getter
@Entity(name = "likes")
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
public class Likes extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@JoinColumn(name = "review_id")
	private Long reviewId;

	@Embedded
	private LikeUserInfo userInfo;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private LikeStatus status = LikeStatus.LIKE;

	public void updateStatus(LikeStatus status) {
		Objects.requireNonNull(status, "상태값은 null일 수 없습니다.");
		this.status = status;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" +
			"id = " + getId() + ", " +
			"userInfo = " + getUserInfo() + ", " +
			"status = " + getStatus() + ", ";
	}
}
