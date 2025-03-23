package app.bottlenote.picks.domain;


import app.bottlenote.common.domain.BaseTimeEntity;
import app.bottlenote.picks.constant.PicksStatus;
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
import org.hibernate.annotations.Comment;

import static lombok.AccessLevel.PROTECTED;

@Builder
@Getter
@Entity(name = "picks")
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PROTECTED)
public class Picks extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("어느 술을 찜했는지")
	@JoinColumn(name = "alcohol_id")
	private Long alcoholId;

	@Comment("누가 찜했는지")
	@JoinColumn(name = "user_id")
	private Long userId;

	@Comment("찜하기 상태")
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private PicksStatus status = PicksStatus.PICK;

	public Picks updateStatus(PicksStatus picked) {
		this.status = picked;
		return this;
	}
}
