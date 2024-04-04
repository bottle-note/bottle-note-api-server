package app.bottlenote.alcohols.domain;

import app.bottlenote.common.domain.BaseEntity;
import app.bottlenote.rating.domain.Rating;
import app.bottlenote.review.domain.Review;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity(name = "alcohols")
public class Alcohols extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Comment("알코올 영어 이름")
	@Column(name = "eng_name", nullable = false)
	private String engName;

	@Comment("알코올 한글 이름")
	@Column(name = "kor_name", nullable = false)
	private String korName;

	@Comment("도수")
	@Column(name = "abv", nullable = false)
	private String abv;

	@Comment("타입")
	@Column(name = "type", nullable = false)
	@Enumerated(EnumType.STRING)
	private AlcoholType type;

	@Comment("하위 카테고리")
	@Column(name = "category", nullable = false)
	private String category;

	@Comment("국가")
	@ManyToOne(fetch = FetchType.LAZY)
	private Country country;

	@Comment("증류소")
	@ManyToOne(fetch = FetchType.LAZY)
	private Distillery distillery;

	@Comment("티어")
	@ManyToOne(fetch = FetchType.LAZY)
	private Tier tier;


	@Comment("캐스트 타입")
	@Column(name = "cask", nullable = true)
	private String cask;

	@OneToMany(mappedBy = "alcohol")
	private List<AlcoholsTastingTags> alcoholsTastingTags = new ArrayList<>();

	@OneToMany(mappedBy = "review", fetch = FetchType.LAZY)
	private List<Review> reviews = new ArrayList<>();

	@OneToMany(mappedBy = "rating",fetch = FetchType.LAZY)
	private List<Rating> rating = new ArrayList<>();
}

