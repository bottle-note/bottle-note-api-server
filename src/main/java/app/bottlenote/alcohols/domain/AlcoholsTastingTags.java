package app.bottlenote.alcohols.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.Comment;

@Comment("알코올과 테이스팅 태그 연관관계 해소 테이블 ")
@Entity(name = "alcohols_tasting_tags")
public class AlcoholsTastingTags {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alcohols_id", nullable = false)
	private Alcohols alcohol;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tasting_tag_id", nullable = false)
	private TastingTag tastingTag;
}
