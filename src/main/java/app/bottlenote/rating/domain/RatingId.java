package app.bottlenote.rating.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

import static lombok.AccessLevel.PROTECTED;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = PROTECTED)
@Embeddable
public class RatingId implements Serializable {

	@Column(name = "user_id")
	private Long userId;
	@Column(name = "alcohol_id")
	private Long alcoholId;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		RatingId ratingId = (RatingId) o;
		return Objects.equals(userId, ratingId.userId) && Objects.equals(alcoholId, ratingId.alcoholId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, alcoholId);
	}

	public static RatingId is(Long userId, Long alcoholId) {
		return new RatingId(userId, alcoholId);
	}
}
