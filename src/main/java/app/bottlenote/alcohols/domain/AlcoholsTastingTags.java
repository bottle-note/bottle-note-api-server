package app.bottlenote.alcohols.domain;

import app.bottlenote.common.domain.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Comment("알코올과 테이스팅 태그 연관관계 해소 테이블 ")
@Entity(name = "alcohol_tasting_tags")
@Table(name = "alcohols_tasting_tags")
public class AlcoholsTastingTags extends BaseTimeEntity {

	@Id
	@Comment("알코올 테이스팅 태그 ID")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("알코올 ID")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alcohol_id", nullable = false)
	private Alcohol alcohol;

	@Comment("테이스팅 태그 ID")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tasting_tag_id", nullable = false)
	private TastingTag tastingTag;
}
