package app.bottlenote.rating.domain;

import app.bottlenote.alcohols.domain.Alcohols;
import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.user.domain.Users;
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

//CREATE TABLE `점수` (
//	`rating_id`	VARCHAR(255)	NOT NULL,
//	`alcohol_id`	VARCHAR(255)	NOT NULL,
//	`user_id`	VARCHAR(255)	NOT NULL,
//	`rating`	VARCHAR(255)	NOT NULL	DEFAULT 0	COMMENT '0점 : 삭제, 0.5:최저점수, 5:최고점수',
//);

	@Id
	@Comment("평가점수 id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "rating_id")
	private Long id;

	@Comment("평가점수 : 0, 0.5, 1 ... 5 (0점 : 삭제와 같다, 0.5:최저점수, 5:최고점수)")
	@Column(name = "rating", nullable = true, columnDefinition = "DOUBLE DEFAULT 0")
	private Double rating;

	@ManyToOne(fetch = FetchType.LAZY)
	private Alcohols alcohols;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private Users user;

}

