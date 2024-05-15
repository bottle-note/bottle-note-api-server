package app.bottlenote.picks.domain;


import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.common.domain.BaseTimeEntity;
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
@Entity(name = "picks")
@NoArgsConstructor(access = PROTECTED)
public class Picks extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alcohol_id")
	private Alcohol alcohol;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Comment("찜하기 상태")
	@Enumerated(EnumType.STRING)
	private PicksStatus status = PicksStatus.PICK;

	@Builder
	public Picks(Long id, Alcohol alcohol, User user, PicksStatus status) {
		this.id = id;
		this.alcohol = alcohol;
		this.user = user;
		this.status = status;
	}

	public Picks updateStatus(PicksStatus picked) {
		this.status = picked;
		return this;
	}
}
