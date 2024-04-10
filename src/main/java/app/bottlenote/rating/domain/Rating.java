package app.bottlenote.rating.domain;

import app.bottlenote.alcohols.domain.Alcohol;
import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("알콜 점수 테이블")
@Entity(name = "rating")
public class Rating extends BaseEntity {

	@Id
	@Comment("평가점수 id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("평가점수 : 0, 0.5, 1 ... 5 (0점 : 삭제와 같다, 0.5:최저점수, 5:최고점수)")
	@Column(name = "rating", nullable = true, columnDefinition = "DOUBLE DEFAULT 0")
	//TODO : 적절한 ENUM 자료형 생성 필요
	private String rating;

	@ManyToOne(fetch = FetchType.LAZY)
	private Alcohol alcohol;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;
}

